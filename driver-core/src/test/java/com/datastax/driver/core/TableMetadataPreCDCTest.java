/*
 * Copyright DataStax, Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.datastax.driver.core;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.fail;

import com.datastax.driver.core.exceptions.SyntaxError;
import org.testng.annotations.Test;

/**
 * Test CDC table option with Cassandra instance NOT supporting CDC. Requires Cassandra version <
 * 3.8.0
 */
@CCMConfig(version = "3.2.1")
public class TableMetadataPreCDCTest extends CCMTestsSupport {

  /**
   * For pre-3.8.0 versions of Cassandra which did not support CDC, ensure that {@link
   * TableOptionsMetadata#isCDC()} returns false and {@link TableMetadata#asCQLQuery()} does not
   * specify CDC.
   *
   * @test_category metadata
   * @jira_ticket JAVA-2539
   */
  @Test(groups = "short")
  public void should_parse_cdc_from_table_options() {
    // given Cassandra version less than 3.8.0
    // create a simple table explicitly setting cdc to true
    // which should fail because Cassandra did not support CDC until 3.8.0
    String cql =
        String.format(
            "CREATE TABLE %s.table_with_cdc_true (\n"
                + "    k text,\n"
                + "    c int,\n"
                + "    v timeuuid,\n"
                + "    PRIMARY KEY (k, c)\n"
                + ") WITH cdc=true;",
            keyspace);
    try {
      session().execute(cql);
      fail("Expected a SyntaxError");
    } catch (SyntaxError e) {
      // Exception verifies we're using Cassandra version which doesn't support CDC
      assertThat(e.getMessage()).contains("Unknown property 'cdc'");
    }

    // create a simple table explicitly setting cdc to false
    // which should fail because Cassandra did not support CDC until 3.8.0
    cql =
        String.format(
            "CREATE TABLE %s.table_with_cdc_false (\n"
                + "    k text,\n"
                + "    c int,\n"
                + "    v timeuuid,\n"
                + "    PRIMARY KEY (k, c)\n"
                + ") WITH cdc=false;",
            keyspace);
    try {
      session().execute(cql);
      fail("Expected a SyntaxError");
    } catch (SyntaxError e) {
      // Exception verifies we're using Cassandra version which doesn't support CDC
      assertThat(e.getMessage()).contains("Unknown property 'cdc'");
    }

    // create a simple table without specifying cdc
    cql =
        String.format(
            "CREATE TABLE %s.table_without_cdc (\n"
                + "    k text,\n"
                + "    c int,\n"
                + "    v timeuuid,\n"
                + "    PRIMARY KEY (k, c)\n"
                + ");",
            keyspace);
    session().execute(cql);
    // when retrieving the table's metadata
    TableMetadata table =
        cluster().getMetadata().getKeyspace(keyspace).getTable("table_without_cdc");
    // then the table's options should have cdc as false
    assertThat(table.getOptions().isCDC()).isEqualTo(false);
    assertThat(table.getOptions().isCDCValid()).isEqualTo(false);
    // when invoking the table's asCQLQuery,
    // then the table's cql creation query should NOT specify cdc
    assertThat(table.asCQLQuery(true)).doesNotContain("cdc =");
  }
}
