package com.datastax.oss.driver;

import com.datastax.oss.driver.shaded.guava.common.collect.Collections2;
import com.datastax.oss.driver.shaded.guava.common.collect.ImmutableList;
import com.datastax.oss.driver.shaded.guava.common.collect.ImmutableMap;
import java.util.Iterator;
import java.util.List;

public class When<T> implements Iterable<Iterable<Object>> {

  public static class ArgsBuilder<T> {

    private final Builder<T> builder;
    private final ImmutableList<Object> args;

    private ArgsBuilder(Builder<T> builder, ImmutableList<Object> args) {
      this.builder = builder;
      this.args = args;
    }

    public Builder<T> expect(T expected) {
      this.builder.expectForArgs(expected, this.args);
      return this.builder;
    }
  }

  public static class Builder<T> {

    private final ImmutableMap.Builder<List<Object>, T> builder;

    private Builder() {
      this.builder = ImmutableMap.<List<Object>, T>builder();
    }

    public Builder<T> expectForArgs(T expected, ImmutableList<Object> args) {
      this.builder.put(args, expected);
      return this;
    }

    public ArgsBuilder<T> args(Object... args) {
      return new ArgsBuilder<T>(this, ImmutableList.copyOf(args));
    }

    public When<T> build() {
      return new When<T>(this.builder.build());
    }
  }

  private final ImmutableMap<List<Object>, T> themap;

  protected When(ImmutableMap<List<Object>, T> themap) {
    this.themap = themap;
  }

  public static <BT> Builder<BT> builder() {
    return new Builder<BT>();
  }

  public When<T> merge(When<T> other) {
    ImmutableMap.Builder<List<Object>, T> builder = ImmutableMap.<List<Object>, T>builder();
    builder.putAll(other.themap);
    builder.putAll(this.themap);
    return new When<T>(builder.build());
  }

  @Override
  public Iterator<Iterable<Object>> iterator() {

    return Collections2.transform(
            this.themap.entrySet(),
            entry -> {
              ImmutableList.Builder<Object> builder =
                  ImmutableList.builder().add(entry.getValue()).addAll(entry.getKey());
              Iterable<Object> rv = builder.build();
              return rv;
            })
        .iterator();
  }
}
