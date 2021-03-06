/**
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */
package org.apache.pulsar.client.impl.schema.generic;

import com.google.protobuf.Descriptors;
import lombok.extern.slf4j.Slf4j;
import org.apache.pulsar.client.api.schema.Field;
import org.apache.pulsar.client.api.schema.GenericRecord;
import org.apache.pulsar.client.api.schema.GenericRecordBuilder;
import org.apache.pulsar.client.api.schema.GenericSchema;
import org.apache.pulsar.client.api.schema.SchemaReader;
import org.apache.pulsar.client.impl.schema.ProtobufNativeSchemaUtils;
import org.apache.pulsar.client.impl.schema.SchemaUtils;
import org.apache.pulsar.common.protocol.schema.BytesSchemaVersion;
import org.apache.pulsar.common.schema.SchemaInfo;

import java.util.List;
import java.util.stream.Collectors;

/**
 * Generic ProtobufNative schema.
 */
@Slf4j
public class GenericProtobufNativeSchema extends AbstractGenericSchema {

    Descriptors.Descriptor descriptor;

    public GenericProtobufNativeSchema(SchemaInfo schemaInfo) {
        this(schemaInfo, true);
    }

    public GenericProtobufNativeSchema(SchemaInfo schemaInfo,
                                       boolean useProvidedSchemaAsReaderSchema) {
        super(schemaInfo, useProvidedSchemaAsReaderSchema);
        this.descriptor = parseProtobufSchema(schemaInfo);
        this.fields = descriptor.getFields()
                .stream()
                .map(f -> new Field(f.getName(), f.getIndex()))
                .collect(Collectors.toList());
        setReader(new GenericProtobufNativeReader(descriptor));
        setWriter(new GenericProtobufNativeWriter());
    }

    @Override
    public List<Field> getFields() {
        return fields;
    }

    @Override
    public GenericRecordBuilder newRecordBuilder() {
        return new ProtobufNativeRecordBuilderImpl(this);
    }

    @Override
    protected SchemaReader<GenericRecord> loadReader(BytesSchemaVersion schemaVersion) {
        SchemaInfo schemaInfo = getSchemaInfoByVersion(schemaVersion.get());
        if (schemaInfo != null) {
            log.info("Load schema reader for version({}), schema is : {}",
                    SchemaUtils.getStringSchemaVersion(schemaVersion.get()),
                    schemaInfo);
            Descriptors.Descriptor recordDescriptor = parseProtobufSchema(schemaInfo);
            Descriptors.Descriptor readerSchemaDescriptor = useProvidedSchemaAsReaderSchema ? descriptor : recordDescriptor;
            return new GenericProtobufNativeReader(
                    readerSchemaDescriptor,
                    schemaVersion.get());
        } else {
            log.warn("No schema found for version({}), use latest schema : {}",
                    SchemaUtils.getStringSchemaVersion(schemaVersion.get()),
                    this.schemaInfo);
            return reader;
        }
    }

    protected static Descriptors.Descriptor parseProtobufSchema(SchemaInfo schemaInfo) {
        return ProtobufNativeSchemaUtils.deserialize(schemaInfo.getSchema());
    }

    public static GenericSchema of(SchemaInfo schemaInfo) {
        return new GenericProtobufNativeSchema(schemaInfo, true);
    }

    public static GenericSchema of(SchemaInfo schemaInfo, boolean useProvidedSchemaAsReaderSchema) {
        return new GenericProtobufNativeSchema(schemaInfo, useProvidedSchemaAsReaderSchema);
    }

    public Descriptors.Descriptor getProtobufNativeSchema() {
        return descriptor;
    }

    @Override
    public boolean supportSchemaVersioning() {
        return true;
    }

}
