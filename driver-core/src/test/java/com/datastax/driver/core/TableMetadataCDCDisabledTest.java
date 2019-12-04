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

import com.datastax.driver.core.utils.CassandraVersion;
import org.testng.annotations.Test;

/**
 * Test CDC table option with Cassandra instance supporting CDC, but cdc is currently disabled. When
 * C* server is configured with cdc_enabled:false (the default), C* silently ignores "WITH cdc"
 * option for new table creation. But DESCRIBE TABLE still reports CDC for tables created while
 * cdc_enabled:true.
 */
@CCMConfig(config = "cdc_enabled:false")
@CassandraVersion(value = "3.8", description = "Requires CASSANDRA-12041 added in 3.8")
public class TableMetadataCDCDisabledTest extends CCMTestsSupport {

  /**
   * Ensures that if a table is configured with change data capture enabled that {@link
   * TableOptionsMetadata#isCDC()} returns true for that table.
   *
   * @test_category metadata
   * @jira_ticket JAVA-1287
   * @jira_ticket CASSANDRA-12041
   */
  @Test(groups = "short")
  public void should_parse_cdc_from_table_options() {
    // given Cassandra version >= 3.8.0
    // create a simple table explicitly setting cdc to true
    // create a simple table with cdc as true.
    String cql =
        String.format(
            "CREATE TABLE %s.table_with_cdc_true (\n"
                + "    k text,\n"
                + "    c int,\n"
                + "    v timeuuid,\n"
                + "    PRIMARY KEY (k, c)\n"
                + ") WITH cdc=true;",
            keyspace);
    session().execute(cql);

    // when retrieving the table's metadata.
    TableMetadata table =
        cluster().getMetadata().getKeyspace(keyspace).getTable("table_with_cdc_true");
    // then the table's options should have cdc as true.
    assertThat(table.getOptions().isCDC()).isEqualTo(false);
    assertThat(table.getOptions().isCDCValid()).isEqualTo(false);
    // when invoking the table's asCQLQuery,
    // then the table's cql creation query should specify cdc as true
    // (because that's how it was really created)
    assertThat(table.asCQLQuery(true)).doesNotContain("cdc = false");

    cql =
        String.format(
            "CREATE TABLE %s.table_with_cdc_false (\n"
                + "    k text,\n"
                + "    c int,\n"
                + "    v timeuuid,\n"
                + "    PRIMARY KEY (k, c)\n"
                + ") WITH cdc=false;",
            keyspace);
    session().execute(cql);
    // when retrieving the table's metadata.
    table = cluster().getMetadata().getKeyspace(keyspace).getTable("table_with_cdc_false");
    // then the table's options should have cdc as true.
    assertThat(table.getOptions().isCDC()).isEqualTo(false);
    assertThat(table.getOptions().isCDCValid()).isEqualTo(false);
    // when invoking the table's asCQLQuery,
    // then the table's cql creation query should specify cdc as true
    // (because that's how it was really created)
    assertThat(table.asCQLQuery(true)).doesNotContain("cdc = false");

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

    // when retrieving the table's metadata.
    table = cluster().getMetadata().getKeyspace(keyspace).getTable("table_without_cdc");
    // then the table's options should have cdc as false because CDC logic is not enabled.
    assertThat(table.getOptions().isCDC()).isEqualTo(false);
    assertThat(table.getOptions().isCDCValid()).isEqualTo(false);
    // when invoking the table's asCQLQuery,
    // then the table's cql creation query should NOT specify cdc
    // (because that's how it was really created)
    assertThat(table.asCQLQuery(true)).doesNotContain("cdc =");
  }
}
