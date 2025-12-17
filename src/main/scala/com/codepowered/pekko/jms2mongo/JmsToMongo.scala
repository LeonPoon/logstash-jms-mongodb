package com.codepowered.pekko.jms2mongo

import com.typesafe.scalalogging.StrictLogging
import org.apache.pekko.actor.ActorSystem
import org.apache.pekko.stream.connectors.jms.JmsConsumerSettings
import org.apache.pekko.stream.connectors.jms.scaladsl.{JmsConsumer, JmsConsumerControl}
import org.apache.pekko.stream.scaladsl.{Flow, Keep, Sink, Source}
import org.apache.pekko.stream.{ActorAttributes, Supervision}
import org.mongodb.scala.MongoCollection
import org.mongodb.scala.bson.BsonDocument

import javax.jms.{ConnectionFactory, Message, TextMessage}
import scala.concurrent.{ExecutionContext, Future}
import scala.util.control.NonFatal

/**
 * Creates a pekko pipeline that reads from jms, transforms the bytes in jms message into BsonDoc, and sends them into a
 * collection in mongodb.
 *
 * Ack the message only if transform and writes to mongo is successful, and Nack for the ones that fail (transform or
 * persist).
 *
 * Can write message out of ordering, so use async for mongodb writes.
 */
class JmsToMongo(
                  connectionFactory: ConnectionFactory,
                  queueName: String,
                  mongoCollection: MongoCollection[BsonDocument],
                  transformer: String => BsonDocument
                )(implicit system: ActorSystem, ec: ExecutionContext) extends StrictLogging {

  def run(): JmsConsumerControl = {
    val jmsSource: Source[Message, JmsConsumerControl] = JmsConsumer(
      JmsConsumerSettings(system, connectionFactory)
        .withQueue(queueName)
    )

    val transformAndPersistFlow = Flow[Message]
      .mapAsync(10) { jmsMessage =>
        Future {
          val bytes: String = jmsMessage match {
            case bm: TextMessage =>
              bm.getText
            case other =>
              throw new IllegalArgumentException(s"Expected BytesMessage, but got ${other.getClass.getName}")
          }
          val bsonDoc = transformer(bytes)
          (bsonDoc, jmsMessage)
        }.flatMap { case (doc, msg) =>
          mongoCollection.insertOne(doc).toFuture().map { _ =>
            logger.info(s"Successfully processed and inserted message with JMSMessageID: ${msg.getJMSMessageID}")
            msg
          }
        }.recoverWith {
          case NonFatal(e) =>
            logger.error(s"Failed to process message with JMSMessageID: ${jmsMessage.getJMSMessageID}", e)
            // Fail the future to trigger supervision strategy
            Future.failed(e)
        }
      }

    val (control, done) = jmsSource
      .via(transformAndPersistFlow)
      .map { jmsMessage =>
        jmsMessage.acknowledge()
        jmsMessage
      }
      .withAttributes(ActorAttributes.supervisionStrategy(Supervision.resumingDecider))
      .toMat(Sink.ignore)(Keep.both)
      .run()

    done.onComplete {
      case scala.util.Success(_) => logger.info("JmsToMongo stream completed successfully.")
      case scala.util.Failure(e) => logger.error("JmsToMongo stream failed.", e)
    }

    control
  }
}
