/**
 * This file is part of Graylog Pipeline Processor.
 *
 * Graylog Pipeline Processor is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * Graylog Pipeline Processor is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with Graylog Pipeline Processor.  If not, see <http://www.gnu.org/licenses/>.
 */
package org.graylog.plugins.pipelineprocessor.functions;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.collect.Maps;
import org.graylog.plugins.pipelineprocessor.BaseParserTest;
import org.graylog.plugins.pipelineprocessor.ast.Rule;
import org.graylog.plugins.pipelineprocessor.ast.functions.Function;
import org.graylog.plugins.pipelineprocessor.functions.json.JsonParse;
import org.graylog.plugins.pipelineprocessor.functions.json.SelectJsonPath;
import org.graylog.plugins.pipelineprocessor.functions.messages.CreateMessage;
import org.graylog.plugins.pipelineprocessor.functions.messages.DropMessage;
import org.graylog.plugins.pipelineprocessor.functions.messages.HasField;
import org.graylog.plugins.pipelineprocessor.functions.messages.RemoveField;
import org.graylog.plugins.pipelineprocessor.functions.messages.SetField;
import org.graylog.plugins.pipelineprocessor.functions.messages.SetFields;
import org.graylog.plugins.pipelineprocessor.functions.strings.Abbreviate;
import org.graylog.plugins.pipelineprocessor.functions.strings.Capitalize;
import org.graylog.plugins.pipelineprocessor.functions.strings.Lowercase;
import org.graylog.plugins.pipelineprocessor.functions.strings.RegexMatch;
import org.graylog.plugins.pipelineprocessor.functions.strings.SwapCase;
import org.graylog.plugins.pipelineprocessor.functions.strings.Uncapitalize;
import org.graylog.plugins.pipelineprocessor.functions.strings.Uppercase;
import org.graylog.plugins.pipelineprocessor.parser.FunctionRegistry;
import org.graylog2.plugin.Message;
import org.graylog2.plugin.Tools;
import org.graylog2.shared.bindings.providers.ObjectMapperProvider;
import org.junit.BeforeClass;
import org.junit.Test;

import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

public class FunctionsSnippetsTest extends BaseParserTest {

    @BeforeClass
    public static void registerFunctions() {
        final Map<String, Function<?>> functions = Maps.newHashMap();

        functions.put(BooleanCoercion.NAME, new BooleanCoercion());
        functions.put(DoubleCoercion.NAME, new DoubleCoercion());
        functions.put(LongCoercion.NAME, new LongCoercion());
        functions.put(StringCoercion.NAME, new StringCoercion());

        // message related functions
        functions.put(HasField.NAME, new HasField());
        functions.put(SetField.NAME, new SetField());
        functions.put(SetFields.NAME, new SetFields());
        functions.put(RemoveField.NAME, new RemoveField());

        functions.put(DropMessage.NAME, new DropMessage());
        functions.put(CreateMessage.NAME, new CreateMessage());
        // TODO needs mock
        //functions.put(RouteToStream.NAME, new RouteToStream()));

        // input related functions
        // TODO needs mock
        //functions.put(FromInput.NAME, new FromInput());

        // generic functions
        functions.put(RegexMatch.NAME, new RegexMatch());

        // string functions
        functions.put(Abbreviate.NAME, new Abbreviate());
        functions.put(Capitalize.NAME, new Capitalize());
        functions.put(Lowercase.NAME, new Lowercase());
        functions.put(SwapCase.NAME, new SwapCase());
        functions.put(Uncapitalize.NAME, new Uncapitalize());
        functions.put(Uppercase.NAME, new Uppercase());

        final ObjectMapper objectMapper = new ObjectMapperProvider().get();
        functions.put(JsonParse.NAME, new JsonParse(objectMapper));
        functions.put(SelectJsonPath.NAME, new SelectJsonPath(objectMapper));

        functionRegistry = new FunctionRegistry(functions);
    }

    @Test
    public void jsonpath() {
        final String json = "{\n" +
                "    \"store\": {\n" +
                "        \"book\": [\n" +
                "            {\n" +
                "                \"category\": \"reference\",\n" +
                "                \"author\": \"Nigel Rees\",\n" +
                "                \"title\": \"Sayings of the Century\",\n" +
                "                \"price\": 8.95\n" +
                "            },\n" +
                "            {\n" +
                "                \"category\": \"fiction\",\n" +
                "                \"author\": \"Evelyn Waugh\",\n" +
                "                \"title\": \"Sword of Honour\",\n" +
                "                \"price\": 12.99\n" +
                "            },\n" +
                "            {\n" +
                "                \"category\": \"fiction\",\n" +
                "                \"author\": \"Herman Melville\",\n" +
                "                \"title\": \"Moby Dick\",\n" +
                "                \"isbn\": \"0-553-21311-3\",\n" +
                "                \"price\": 8.99\n" +
                "            },\n" +
                "            {\n" +
                "                \"category\": \"fiction\",\n" +
                "                \"author\": \"J. R. R. Tolkien\",\n" +
                "                \"title\": \"The Lord of the Rings\",\n" +
                "                \"isbn\": \"0-395-19395-8\",\n" +
                "                \"price\": 22.99\n" +
                "            }\n" +
                "        ],\n" +
                "        \"bicycle\": {\n" +
                "            \"color\": \"red\",\n" +
                "            \"price\": 19.95\n" +
                "        }\n" +
                "    },\n" +
                "    \"expensive\": 10\n" +
                "}";

        final Rule rule = parser.parseRule(ruleForTest());
        final Message message = evaluateRule(rule, new Message(json, "test", Tools.nowUTC()));

        assertThat(message.hasField("author_first")).isTrue();
        assertThat(message.hasField("author_last")).isTrue();

    }
}
