// Generated by the protocol buffer compiler.  DO NOT EDIT!
// source: scalardb.proto

package com.scalar.db.rpc;

public interface CreateTableRequestOrBuilder extends
    // @@protoc_insertion_point(interface_extends:rpc.CreateTableRequest)
    com.google.protobuf.MessageOrBuilder {

  /**
   * <code>string namespace = 1;</code>
   * @return The namespace.
   */
  java.lang.String getNamespace();
  /**
   * <code>string namespace = 1;</code>
   * @return The bytes for namespace.
   */
  com.google.protobuf.ByteString
      getNamespaceBytes();

  /**
   * <code>string table = 2;</code>
   * @return The table.
   */
  java.lang.String getTable();
  /**
   * <code>string table = 2;</code>
   * @return The bytes for table.
   */
  com.google.protobuf.ByteString
      getTableBytes();

  /**
   * <code>.rpc.TableMetadata table_metadata = 3;</code>
   * @return Whether the tableMetadata field is set.
   */
  boolean hasTableMetadata();
  /**
   * <code>.rpc.TableMetadata table_metadata = 3;</code>
   * @return The tableMetadata.
   */
  com.scalar.db.rpc.TableMetadata getTableMetadata();
  /**
   * <code>.rpc.TableMetadata table_metadata = 3;</code>
   */
  com.scalar.db.rpc.TableMetadataOrBuilder getTableMetadataOrBuilder();

  /**
   * <code>map&lt;string, string&gt; options = 4;</code>
   */
  int getOptionsCount();
  /**
   * <code>map&lt;string, string&gt; options = 4;</code>
   */
  boolean containsOptions(
      java.lang.String key);
  /**
   * Use {@link #getOptionsMap()} instead.
   */
  @java.lang.Deprecated
  java.util.Map<java.lang.String, java.lang.String>
  getOptions();
  /**
   * <code>map&lt;string, string&gt; options = 4;</code>
   */
  java.util.Map<java.lang.String, java.lang.String>
  getOptionsMap();
  /**
   * <code>map&lt;string, string&gt; options = 4;</code>
   */
  /* nullable */
java.lang.String getOptionsOrDefault(
      java.lang.String key,
      /* nullable */
java.lang.String defaultValue);
  /**
   * <code>map&lt;string, string&gt; options = 4;</code>
   */
  java.lang.String getOptionsOrThrow(
      java.lang.String key);

  /**
   * <code>bool if_not_exists = 5;</code>
   * @return The ifNotExists.
   */
  boolean getIfNotExists();
}
