CREATE TABLE `gateway_routes` (
       `id` VARCHAR(100) NOT NULL,
       `uri` TEXT NOT NULL,
       `predicates` TEXT NOT NULL,
       `filters` TEXT,
       `route_order` INT NOT NULL DEFAULT 0,
       PRIMARY KEY (`id`)
     ) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;
