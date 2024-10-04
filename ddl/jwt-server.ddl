--
-- Table structure for table `errors`
--

CREATE TABLE IF NOT EXISTS `errors` (
  `id` int NOT NULL AUTO_INCREMENT,
  `user` varchar(20) NOT NULL,
  `date` timestamp NULL,
  `type` int DEFAULT '0',
  `ip_addr` varchar(256) DEFAULT NULL,
  `hostname` varchar(256) DEFAULT NULL,
  PRIMARY KEY (`id`),
  KEY `id` (`id`)
);


--
-- Table structure for table `tokens`
--

CREATE TABLE IF NOT EXISTS `tokens` (
  `user` varchar(20) NOT NULL,
  `audience` varchar(20) NOT NULL,
  `access_token` text,
  `refresh_token` text,
  `iv` varchar(128) NOT NULL,
  PRIMARY KEY (`user`,`audience`)
);

--
-- Table structure for table `errors`
--

CREATE TABLE IF NOT EXISTS `issues` (
  `id` int NOT NULL AUTO_INCREMENT,
  `user` varchar(20) NOT NULL,
  `date` timestamp NULL,
  `ip_addr` varchar(256) DEFAULT NULL,
  `hostname` varchar(256) DEFAULT NULL,
  `type` int DEFAULT '0',
  PRIMARY KEY (`id`),
  KEY `id` (`id`)
);