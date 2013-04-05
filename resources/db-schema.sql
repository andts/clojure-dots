CREATE DATABASE  IF NOT EXISTS `dots` /*!40100 DEFAULT CHARACTER SET utf8 */;
USE `dots`;

--
-- Table structure for table `dots`
--

DROP TABLE IF EXISTS `dots`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!40101 SET character_set_client = utf8 */;
CREATE TABLE `dots` (
  `dot-id` bigint(20) NOT NULL,
  `field-id` bigint(20) NOT NULL,
  `x` int(11) NOT NULL,
  `y` int(11) NOT NULL,
  PRIMARY KEY (`dot-id`),
  UNIQUE KEY `dot-id_UNIQUE` (`dot-id`),
  KEY `field-id-fk_idx` (`field-id`),
  CONSTRAINT `field-id-fk` FOREIGN KEY (`field-id`) REFERENCES `fields` (`field-id`) ON DELETE NO ACTION ON UPDATE NO ACTION
) ENGINE=InnoDB DEFAULT CHARSET=utf8;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `fields`
--

DROP TABLE IF EXISTS `fields`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!40101 SET character_set_client = utf8 */;
CREATE TABLE `fields` (
  `field-id` bigint(20) NOT NULL,
  `width` int(11) DEFAULT NULL,
  `height` int(11) DEFAULT NULL,
  `game-id` bigint(20) DEFAULT NULL,
  PRIMARY KEY (`field-id`),
  KEY `game-id-fk_idx` (`game-id`),
  CONSTRAINT `game-id-fk` FOREIGN KEY (`game-id`) REFERENCES `games` (`game-id`) ON DELETE NO ACTION ON UPDATE NO ACTION
) ENGINE=InnoDB DEFAULT CHARSET=utf8;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `games`
--

DROP TABLE IF EXISTS `games`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!40101 SET character_set_client = utf8 */;
CREATE TABLE `games` (
  `game-id` bigint(20) NOT NULL,
  `player1-id` bigint(20) DEFAULT NULL,
  `player2-id` bigint(20) DEFAULT NULL,
  PRIMARY KEY (`game-id`),
  UNIQUE KEY `id_UNIQUE` (`game-id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8;
/*!40101 SET character_set_client = @saved_cs_client */;
/*!40103 SET TIME_ZONE=@OLD_TIME_ZONE */;

/*!40101 SET SQL_MODE=@OLD_SQL_MODE */;
/*!40014 SET FOREIGN_KEY_CHECKS=@OLD_FOREIGN_KEY_CHECKS */;
/*!40014 SET UNIQUE_CHECKS=@OLD_UNIQUE_CHECKS */;
/*!40101 SET CHARACTER_SET_CLIENT=@OLD_CHARACTER_SET_CLIENT */;
/*!40101 SET CHARACTER_SET_RESULTS=@OLD_CHARACTER_SET_RESULTS */;
/*!40101 SET COLLATION_CONNECTION=@OLD_COLLATION_CONNECTION */;
/*!40111 SET SQL_NOTES=@OLD_SQL_NOTES */;

-- Dump completed on 2013-04-03 17:28:13
