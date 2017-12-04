package com.sksamuel.elastic4s.nodes

import com.sksamuel.elastic4s.http.ElasticDsl
import com.sksamuel.elastic4s.testkit.DiscoveryLocalNodeProvider
import org.scalatest.{Matchers, WordSpec}

class NodesStatsHttpTest extends WordSpec with Matchers with DiscoveryLocalNodeProvider with ElasticDsl {

  "node stats request" should {
    "return os information" in {
      val stats = http.execute {
        nodeStats()
      }.await

      stats.get.clusterName should be("localnode-cluster")
      stats.get.nodes.size > 0 shouldBe true
    }
  }
}
