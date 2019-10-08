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
package com.datastax.oss.driver.internal.core.metadata.schema.queries;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.datastax.oss.driver.When;
import com.datastax.oss.driver.api.core.Version;
import com.datastax.oss.driver.api.core.config.DriverConfig;
import com.datastax.oss.driver.api.core.config.DriverExecutionProfile;
import com.datastax.oss.driver.api.core.metadata.Node;
import com.datastax.oss.driver.internal.core.channel.DriverChannel;
import com.datastax.oss.driver.internal.core.context.InternalDriverContext;
import com.tngtech.java.junit.dataprovider.DataProvider;
import com.tngtech.java.junit.dataprovider.DataProviderRunner;
import com.tngtech.java.junit.dataprovider.UseDataProvider;
import java.util.concurrent.CompletableFuture;
import org.junit.Test;
import org.junit.runner.RunWith;

@RunWith(DataProviderRunner.class)
public class DefaultSchemaQueriesFactoryTest {

  enum Expected {
    CASS_21(Cassandra21SchemaQueries.class),
    CASS_22(Cassandra22SchemaQueries.class),
    CASS_3(Cassandra3SchemaQueries.class),
    CASS_4(Cassandra4SchemaQueries.class);

    Class<? extends SchemaQueries> clz;

    Expected(Class<? extends SchemaQueries> clz) {
      this.clz = clz;
    }

    public Class<? extends SchemaQueries> getClz() {
      return clz;
    }
  }

  private static When<Expected> whenCassandra =
      When.<Expected>builder()
          .args("2.1.0")
          .expect(Expected.CASS_21)
          .args("2.2.0")
          .expect(Expected.CASS_22)
          .args("2.2.1")
          .expect(Expected.CASS_22)
          /* Not a real version, just documenting behaviour of existing impl */
          .args("2.3.0")
          .expect(Expected.CASS_22)
          /* We now return you to real versions */
          .args("3.0.0")
          .expect(Expected.CASS_3)
          .args("3.0.1")
          .expect(Expected.CASS_3)
          .args("3.1.0")
          .expect(Expected.CASS_3)
          .args("4.0.0")
          .expect(Expected.CASS_4)
          .args("4.0.1")
          .expect(Expected.CASS_4)
          .args("4.1.0")
          .expect(Expected.CASS_4)
          .build();

  private static When<Expected> whenDse =
      When.<Expected>builder()
          /* DSE 6.0.0 */
          .args("4.0.0.2284")
          .expect(Expected.CASS_3)
          /* DSE 6.0.1 */
          .args("4.0.0.2349")
          .expect(Expected.CASS_3)
          /* DSE 6.0.2 moved to DSE version (minus dots) in an extra element */
          .args("4.0.0.602")
          .expect(Expected.CASS_3)
          /* DSE 6.7.0 continued with the same idea */
          .args("4.0.0.670")
          .expect(Expected.CASS_3)
          /* DSE 6.8.0 does the same */
          .args("4.0.0.680")
          .expect(Expected.CASS_3)
          .build();

  @DataProvider(format = "%m %p[1] => %p[0]")
  public static Iterable<Iterable<Object>> expected() {

    return whenCassandra.merge(whenDse);
  }

  @Test
  @UseDataProvider("expected")
  public void should_return_correct_schema_queries_impl(Expected expected, String version) {

    final Node mockNode = mock(Node.class);
    when(mockNode.getCassandraVersion()).thenReturn(Version.parse(version));

    DefaultSchemaQueriesFactory factory = buildFactory();
    SchemaQueries queries =
        factory.newInstance(mockNode, mock(DriverChannel.class), mock(CompletableFuture.class));
    assertThat(queries.getClass()).isEqualTo(expected.getClz());
  }

  private DefaultSchemaQueriesFactory buildFactory() {

    final DriverExecutionProfile mockProfile = mock(DriverExecutionProfile.class);
    final DriverConfig mockConfig = mock(DriverConfig.class);
    when(mockConfig.getDefaultProfile()).thenReturn(mockProfile);
    final InternalDriverContext mockInternalCtx = mock(InternalDriverContext.class);
    when(mockInternalCtx.getConfig()).thenReturn(mockConfig);

    return new DefaultSchemaQueriesFactory(mockInternalCtx);
  }
}
