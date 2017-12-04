package com.sksamuel.elastic4s.update

import com.sksamuel.elastic4s.http.ElasticDsl
import com.sksamuel.elastic4s.testkit.DiscoveryLocalNodeProvider
import com.sksamuel.elastic4s.{ElasticApi, RefreshPolicy}
import org.scalatest.{FlatSpec, Matchers}

import scala.util.Try

class UpdateTest extends FlatSpec with Matchers with ElasticDsl with DiscoveryLocalNodeProvider {

  Try {
    http.execute {
      ElasticApi.deleteIndex("hans")
    }.await
  }

  http.execute {
    createIndex("hans").mappings(
      mapping("albums").fields(
        textField("name").stored(true)
      )
    )
  }.await

  http.execute(
    indexInto("hans/albums") fields "name" -> "intersteller" id "5" refresh RefreshPolicy.Immediate
  ).await

  "an update request" should "support field based update" in {

    http.execute {
      update("5").in("hans" / "albums").doc(
        "name" -> "man of steel"
      ).refresh(RefreshPolicy.Immediate)
    }.await.get.result shouldBe "updated"

    http.execute {
      get("5").from("hans/albums").storedFields("name")
    }.await.get.storedFieldsAsMap shouldBe Map("name" -> List("man of steel"))
  }

  it should "support string based update" in {
    http.execute {
      update("5").in("hans" / "albums").doc(""" { "name" : "inception" } """).refresh(RefreshPolicy.Immediate)
    }.await.get.result shouldBe "updated"

    http.execute {
      get("5").from("hans/albums").storedFields("name")
    }.await.get.storedFieldsAsMap shouldBe Map("name" -> List("inception"))
  }

  it should "support field based upsert" in {

    http.execute {
      update("5").in("hans/albums").docAsUpsert(
        "name" -> "batman"
      ).refresh(RefreshPolicy.Immediate)
    }.await.get.result shouldBe "updated"

    http.execute {
      get("5").from("hans" / "albums").storedFields("name")
    }.await.get.storedFieldsAsMap shouldBe Map("name" -> List("batman"))
  }

  it should "support string based upsert" in {
    http.execute {
      update("44").in("hans" / "albums").docAsUpsert(""" { "name" : "pirates of the caribbean" } """).refresh(RefreshPolicy.Immediate)
    }.await.get.result shouldBe "created"

    http.execute {
      get("44").from("hans/albums").storedFields("name")
    }.await.get.storedFieldsAsMap shouldBe Map("name" -> List("pirates of the caribbean"))
  }

  it should "keep existing fields with partial update" in {

    http.execute {
      update("5").in("hans/albums").docAsUpsert(
        "length" -> 12.34
      ).refresh(RefreshPolicy.Immediate)
    }.await.get.result shouldBe "updated"

    http.execute {
      get("5").from("hans/albums").storedFields("name")
    }.await.get.storedFieldsAsMap shouldBe Map("name" -> List("batman"))
  }

  it should "insert non existent doc when using docAsUpsert" in {
    http.execute {
      update("14").in("hans/albums").docAsUpsert(
        "name" -> "hunt for the red october"
      )
    }.await.get.result shouldBe "created"
  }

  it should "return errors when the index does not exist" in {
    val resp = http.execute {
      update("5").in("wowooasdsad" / "qweqwe").doc(
        "name" -> "gladiator"
      )
    }.await
    resp.error.`type` shouldBe "document_missing_exception"
    resp.error.reason should include("document missing")
  }

  it should "return errors when the id does not exist" in {
    val resp = http.execute {
      update("234234").in("hans/albums").doc(
        "name" -> "gladiator"
      )
    }.await
    resp.error.`type` shouldBe "document_missing_exception"
    resp.error.reason should include("document missing")
  }

  it should "not return source by default" in {
    val resp = http.execute {
      update("666").in("hans/albums").docAsUpsert(
        "name" -> "dunkirk"
      ).refresh(RefreshPolicy.Immediate)
    }.await
    resp.get.source shouldBe Map.empty
  }

  it should "return source when specified" in {
    val resp = http.execute {
      update("667").in("hans/albums").docAsUpsert(
        "name" -> "thin red line"
      ).refresh(RefreshPolicy.Immediate).fetchSource(true)
    }.await
    resp.get.source shouldBe Map("name" -> "thin red line")
  }

  it should "include the original json" in {
    val resp = http.execute {
      update("555").in("hans/albums").docAsUpsert(
        "name" -> "spider man"
      ).refresh(RefreshPolicy.Immediate).fetchSource(true)
    }.await
    resp.body.get shouldBe """{"_index":"hans","_type":"albums","_id":"555","_version":1,"result":"created","forced_refresh":true,"_shards":{"total":2,"successful":1,"failed":0},"_seq_no":3,"_primary_term":1,"get":{"found":true,"_source":{"name":"spider man"}}}"""
  }
}
