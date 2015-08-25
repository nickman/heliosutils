CREATE VIEW "tsdb-uid" ( "id".val VARBINARY) --default_column_family='t'

DROP VIEW "tsdb-uid"

CREATE VIEW "tsdb-uid" (
  --PK VARCHAR PRIMARY KEY
  PK VARBINARY PRIMARY KEY
--  ,"name"."value" VARCHAR
--  ,"timestamp"."value" VARBINARY
  ,"name"."tagv" VARCHAR
  ,"id"."tagv" VARBINARY  
  ,"name"."tagv_meta" VARCHAR

  ,"name"."tagk" VARCHAR
  ,"id"."tagk" VARBINARY  
  ,"name"."tagk_meta" VARCHAR

  ,"name"."metrics" VARCHAR
  ,"id"."metrics" VARBINARY 
  ,"name"."metrics_meta" VARCHAR
) default_column_family='id'

select TOHEX(PK, 0, 3), "name"."tagv", "id"."tagv" ITAGV, "name"."tagv_meta" from "tsdb-uid"   where "name"."tagv" is not null and "name"."tagv" = '17'

SELECT PK UID,  "name"."tagv" NAME, "id"."tagv" ID, "name"."tagv_meta" META FROM "tsdb-uid" WHERE "name"."tagv" is not null

select TOHEX(PK), "name"."tagk", "id"."tagk", "name"."tagk_meta" from "tsdb-uid"   where "name"."tagk" is not null

select TOHEX(PK, 0, 3), "name"."metrics", "id"."metrics", "name"."metrics_meta" from "tsdb-uid"   where "name"."metrics" is not null

select * from "tsdb-uid" 


where PK = '/boot'
UNION ALL
select * from "tsdb-uid" where PK = '