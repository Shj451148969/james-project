/****************************************************************
 * Licensed to the Apache Software Foundation (ASF) under one   *
 * or more contributor license agreements.  See the NOTICE file *
 * distributed with this work for additional information        *
 * regarding copyright ownership.  The ASF licenses this file   *
 * to you under the Apache License, Version 2.0 (the            *
 * "License"); you may not use this file except in compliance   *
 * with the License.  You may obtain a copy of the License at   *
 *                                                              *
 *   http://www.apache.org/licenses/LICENSE-2.0                 *
 *                                                              *
 * Unless required by applicable law or agreed to in writing,   *
 * software distributed under the License is distributed on an  *
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY       *
 * KIND, either express or implied.  See the License for the    *
 * specific language governing permissions and limitations      *
 * under the License.                                           *
 ****************************************************************/

package org.apache.james.mailets;

import static org.apache.james.mailets.configuration.Constants.DEFAULT_DOMAIN;
import static org.apache.james.mailets.configuration.Constants.LOCALHOST_IP;
import static org.apache.james.mailets.configuration.Constants.PASSWORD;
import static org.apache.james.mailets.configuration.Constants.awaitAtMostOneMinute;
import static org.assertj.core.api.Assertions.assertThat;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import org.apache.james.MemoryJamesServerMain;
import org.apache.james.jdkim.api.PublicKeyRecordRetriever;
import org.apache.james.jdkim.mailets.DKIMSign;
import org.apache.james.jdkim.mailets.DKIMVerify;
import org.apache.james.jdkim.mailets.MockPublicKeyRecordRetriever;
import org.apache.james.mailets.configuration.CommonProcessors;
import org.apache.james.mailets.configuration.MailetConfiguration;
import org.apache.james.mailets.configuration.MailetContainer;
import org.apache.james.mailets.configuration.ProcessorConfiguration;
import org.apache.james.modules.protocols.ImapGuiceProbe;
import org.apache.james.modules.protocols.SmtpGuiceProbe;
import org.apache.james.probe.DataProbe;
import org.apache.james.transport.mailets.ExtractAttributeStub;
import org.apache.james.transport.matchers.All;
import org.apache.james.utils.DataProbeImpl;
import org.apache.james.utils.IMAPMessageReader;
import org.apache.james.utils.SMTPMessageSender;
import org.junit.After;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;

public class DKIMIntegrationTest {

    private static final String FROM_LOCAL_PART = "fromUser";
    private static final String FROM = FROM_LOCAL_PART + "@" + DEFAULT_DOMAIN;
    private static final String RECIPIENT_LOCAL_PART = "touser";
    private static final String RECIPIENT = RECIPIENT_LOCAL_PART + "@" + DEFAULT_DOMAIN;

    private static final String TESTING_PEM = "-----BEGIN RSA PRIVATE KEY-----\r\n" +
        "MIICXAIBAAKBgQDYDaYKXzwVYwqWbLhmuJ66aTAN8wmDR+rfHE8HfnkSOax0oIoT\r\n" +
        "M5zquZrTLo30870YMfYzxwfB6j/Nz3QdwrUD/t0YMYJiUKyWJnCKfZXHJBJ+yfRH\r\n" +
        "r7oW+UW3cVo9CG2bBfIxsInwYe175g9UjyntJpWueqdEIo1c2bhv9Mp66QIDAQAB\r\n" +
        "AoGBAI8XcwnZi0Sq5N89wF+gFNhnREFo3rsJDaCY8iqHdA5DDlnr3abb/yhipw0I\r\n" +
        "/1HlgC6fIG2oexXOXFWl+USgqRt1kTt9jXhVFExg8mNko2UelAwFtsl8CRjVcYQO\r\n" +
        "cedeH/WM/mXjg2wUqqZenBmlKlD6vNb70jFJeVaDJ/7n7j8BAkEA9NkH2D4Zgj/I\r\n" +
        "OAVYccZYH74+VgO0e7VkUjQk9wtJ2j6cGqJ6Pfj0roVIMUWzoBb8YfErR8l6JnVQ\r\n" +
        "bfy83gJeiQJBAOHk3ow7JjAn8XuOyZx24KcTaYWKUkAQfRWYDFFOYQF4KV9xLSEt\r\n" +
        "ycY0kjsdxGKDudWcsATllFzXDCQF6DTNIWECQEA52ePwTjKrVnLTfCLEG4OgHKvl\r\n" +
        "Zud4amthwDyJWoMEH2ChNB2je1N4JLrABOE+hk+OuoKnKAKEjWd8f3Jg/rkCQHj8\r\n" +
        "mQmogHqYWikgP/FSZl518jV48Tao3iXbqvU9Mo2T6yzYNCCqIoDLFWseNVnCTZ0Q\r\n" +
        "b+IfiEf1UeZVV5o4J+ECQDatNnS3V9qYUKjj/krNRD/U0+7eh8S2ylLqD3RlSn9K\r\n" +
        "tYGRMgAtUXtiOEizBH6bd/orzI9V9sw8yBz+ZqIH25Q=\r\n" +
        "-----END RSA PRIVATE KEY-----\r\n";
    private static final MailetConfiguration DKIMSIGN_MAILET = MailetConfiguration.builder()
        .matcher(All.class)
        .mailet(DKIMSign.class)
        .addProperty(
            "signatureTemplate",
            "v=1; s=selector; d=example.com; h=from:to:received:received; a=rsa-sha256; bh=; b=;")
        .addProperty("privateKey", TESTING_PEM)
        .build();

    private static final MailetConfiguration DKIMVERIFY_MAILET = MailetConfiguration.builder()
        .matcher(All.class)
        .mailet(DKIMVerify.class)
        .build();


    private static final MailetConfiguration STUB_MAILET = MailetConfiguration.builder()
        .matcher(All.class)
        .mailet(ExtractAttributeStub.class)
        .addProperty("attributeName", DKIMVerify.DKIM_AUTH_RESULT.asString())
        .build();

    private static final PublicKeyRecordRetriever MOCK_PUBLIC_KEY_RECORD_RETRIEVER = new MockPublicKeyRecordRetriever(
            "v=DKIM1; k=rsa; p=MIGfMA0GCSqGSIb3DQEBAQUAA4GNADCBiQKBgQDYDaYKXzwVYwqWbLhmuJ66aTAN8wmDR+rfHE8HfnkSOax0oIoTM5zquZrTLo30870YMfYzxwfB6j/Nz3QdwrUD/t0YMYJiUKyWJnCKfZXHJBJ+yfRHr7oW+UW3cVo9CG2bBfIxsInwYe175g9UjyntJpWueqdEIo1c2bhv9Mp66QIDAQAB;",
            "selector", "example.com");

    @Rule
    public TemporaryFolder temporaryFolder = new TemporaryFolder();
    @Rule
    public IMAPMessageReader imapMessageReader = new IMAPMessageReader();
    @Rule
    public SMTPMessageSender messageSender = new SMTPMessageSender(DEFAULT_DOMAIN);

    private TemporaryJamesServer jamesServer;
    private List<Optional<String>> dkimAuthResults;

    @Before
    public void setup() throws Exception {
        dkimAuthResults = new ArrayList<>();
        ExtractAttributeStub.setDkimAuthResultInspector(value -> dkimAuthResults.add(value.map(result -> (String) result)));
        MailetContainer.Builder mailetContainer = TemporaryJamesServer.SIMPLE_MAILET_CONTAINER_CONFIGURATION
            .putProcessor(ProcessorConfiguration.transport()
                .addMailet(DKIMSIGN_MAILET)
                .addMailet(DKIMVERIFY_MAILET)
                .addMailet(STUB_MAILET)
                .addMailetsFrom(CommonProcessors.transport()));

        jamesServer = TemporaryJamesServer
            .builder()
            .withBase(MemoryJamesServerMain.IN_MEMORY_SERVER_AGGREGATE_MODULE)
            .withOverrides(binder -> binder.bind(PublicKeyRecordRetriever.class).toInstance(MOCK_PUBLIC_KEY_RECORD_RETRIEVER))
            .withMailetContainer(mailetContainer)
            .build(temporaryFolder);

        DataProbe dataProbe = jamesServer.getProbe(DataProbeImpl.class);
        dataProbe.addDomain(DEFAULT_DOMAIN);

        dataProbe.addUser(RECIPIENT, PASSWORD);
    }

    @After
    public void tearDown() {
        jamesServer.shutdown();
    }

    @Test
    public void incomingMessageShouldBeReceivedSignedAndChecked() throws Exception {
        messageSender.connect(LOCALHOST_IP, jamesServer.getProbe(SmtpGuiceProbe.class).getSmtpPort())
            .sendMessage(FROM, RECIPIENT);

        imapMessageReader.connect(LOCALHOST_IP, jamesServer.getProbe(ImapGuiceProbe.class).getImapPort())
            .login(RECIPIENT, PASSWORD)
            .select(IMAPMessageReader.INBOX)
            .awaitMessage(awaitAtMostOneMinute);

        assertThat(dkimAuthResults)
                .hasSize(1);
        assertThat(dkimAuthResults.get(0))
                .hasValueSatisfying(result -> assertThat(result).startsWith("pass"));

        assertThat(imapMessageReader.readFirstMessageHeaders())
                .contains("DKIM-Signature");
    }
}
