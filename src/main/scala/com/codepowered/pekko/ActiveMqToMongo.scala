package com.codepowered.pekko

import com.codepowered.pekko.jms2mongo.JmsToMongo
import org.apache.activemq.ActiveMQConnectionFactory
import org.apache.activemq.broker.BrokerService
import org.apache.pekko.actor.ActorSystem
import org.mongodb.scala.bson.{BsonDocument, BsonString}
import org.mongodb.scala.{ConnectionString, MongoClient, MongoClientSettings, MongoCollection, MongoCredential}

import javax.jms.ConnectionFactory
import scala.concurrent.ExecutionContext.Implicits.global

class ActiveMqToMongo extends (String => BsonDocument) {
  def apply(s: String): BsonDocument = BsonDocument("s" -> BsonString(s))

  /**
   * Starts a non-persistent, embedded ActiveMQ broker.
   * - Remote clients connect via TCP to send messages.
   * - The internal consumer connects via the high-performance VM transport.
   * Starts a Pekko actor system and a MongoDB connection, then runs the JmsToMongo pipeline.
   */
  def run(): Unit = {

    val brokerName = "localhost"
    val remoteProducerUrl = s"tcp://$brokerName:61616"
    // Use the VM transport for efficient in-JVM communication
    val internalConsumerUrl = s"vm://$brokerName?create=false"

    val mongodbUri = "mongodb://localhost:27017"
    val mongodbDbName = "mydb"
    val collectionName = "mycollection"
    val mongodbUser = "SomeUser"
    val mongodbPassword = "Somepassword"

    implicit val actorSystem: ActorSystem = ActorSystem("ActiveMqToMongo")

    // Configure and start the embedded ActiveMQ broker
    val broker = new BrokerService()
    broker.setBrokerName(brokerName)
    broker.setPersistent(false) // Disable message persistence
    broker.setUseJmx(false) // Disable JMX for performance, not needed for this use case
    broker.addConnector(remoteProducerUrl) // For remote producers
    broker.start()

    /**
     * Connects to the embedded broker using the VM transport for efficiency,
     * as the consumer is in the same JVM. This avoids TCP overhead.
     */
    val connectionFactory: ConnectionFactory = new ActiveMQConnectionFactory(internalConsumerUrl)

    val queueName: String = "my-queue"

    // Setup MongoDB client
    val credential = MongoCredential.createCredential(mongodbUser, mongodbDbName, mongodbPassword.toCharArray)
    val mongoClientSettings = MongoClientSettings.builder()
      .applyConnectionString(ConnectionString(mongodbUri))
      // .credential(credential) // Uncomment if your MongoDB requires authentication
      .build()
    val mongoClient = MongoClient(mongoClientSettings)
    val database = mongoClient.getDatabase(mongodbDbName)
    val mongoCollection: MongoCollection[BsonDocument] = database.getCollection(collectionName)

    new JmsToMongo(connectionFactory, queueName, mongoCollection, this).run()
  }
}

object ActiveMqToMongo {
  def main(args: Array[String]): Unit = {
    new ActiveMqToMongo().run()
  }
}
