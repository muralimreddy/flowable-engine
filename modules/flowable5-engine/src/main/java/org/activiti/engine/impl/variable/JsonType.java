/* Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * 
 *      http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.activiti.engine.impl.variable;

import org.flowable.variable.service.impl.types.ValueFields;
import org.flowable.variable.service.impl.types.VariableType;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

/**
 * @author Tijs Rademakers
 */
public class JsonType implements VariableType {

    private static final Logger LOGGER = LoggerFactory.getLogger(JsonType.class);

    protected final int maxLength;
    protected ObjectMapper objectMapper;

    public JsonType(int maxLength, ObjectMapper objectMapper) {
        this.maxLength = maxLength;
        this.objectMapper = objectMapper;
    }

    @Override
    public String getTypeName() {
        return "json";
    }

    @Override
    public boolean isCachable() {
        return true;
    }

    @Override
    public Object getValue(ValueFields valueFields) {
        JsonNode jsonValue = null;
        if (valueFields.getTextValue() != null && valueFields.getTextValue().length() > 0) {
            try {
                jsonValue = objectMapper.readTree(valueFields.getTextValue());
            } catch (Exception e) {
                LOGGER.error("Error reading json variable {}", valueFields.getName(), e);
            }
        }
        return jsonValue;
    }

    @Override
    public void setValue(Object value, ValueFields valueFields) {
        valueFields.setTextValue(value != null ? value.toString() : null);
    }

    @Override
    public boolean isAbleToStore(Object value) {
        if (value == null) {
            return true;
        }
        if (JsonNode.class.isAssignableFrom(value.getClass())) {
            JsonNode jsonValue = (JsonNode) value;
            return jsonValue.toString().length() <= maxLength;
        }
        return false;
    }
}
