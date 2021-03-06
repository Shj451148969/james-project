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

package org.apache.james.imap.message.response;

import org.apache.james.imap.api.ImapCommand;
import org.apache.james.imap.api.Tag;
import org.apache.james.imap.api.display.HumanReadableText;
import org.apache.james.imap.api.message.response.StatusResponse;
import org.apache.james.imap.api.message.response.StatusResponse.ResponseCode;
import org.apache.james.imap.api.message.response.StatusResponseFactory;

public abstract class AbstractStatusResponseFactory implements StatusResponseFactory {

    AbstractStatusResponseFactory() {
    }

    protected abstract StatusResponse createResponse(StatusResponse.Type type, Tag tag, ImapCommand command, HumanReadableText displayTextKey, ResponseCode code);

    @Override
    public StatusResponse bye(HumanReadableText displayTextKey, ResponseCode code) {
        return createResponse(StatusResponse.Type.BYE, null, null, displayTextKey, code);
    }

    @Override
    public StatusResponse bye(HumanReadableText displayTextKey) {
        return bye(displayTextKey, null);
    }

    @Override
    public StatusResponse preauth(HumanReadableText displayTextKey, ResponseCode code) {
        return createResponse(StatusResponse.Type.PREAUTH, null, null, displayTextKey, code);
    }

    @Override
    public StatusResponse preauth(HumanReadableText displayTextKey) {
        return preauth(displayTextKey, null);
    }

    @Override
    public StatusResponse taggedBad(Tag tag, ImapCommand command, HumanReadableText displayTextKey, ResponseCode code) {
        return createResponse(StatusResponse.Type.BAD, tag, command, displayTextKey, code);
    }

    @Override
    public StatusResponse taggedBad(Tag tag, ImapCommand command, HumanReadableText displayTextKey) {
        return taggedBad(tag, command, displayTextKey, null);
    }

    @Override
    public StatusResponse taggedNo(Tag tag, ImapCommand command, HumanReadableText displayTextKey, ResponseCode code) {
        return createResponse(StatusResponse.Type.NO, tag, command, displayTextKey, code);
    }

    @Override
    public StatusResponse taggedNo(Tag tag, ImapCommand command, HumanReadableText displayTextKey) {
        return taggedNo(tag, command, displayTextKey, null);
    }

    @Override
    public StatusResponse taggedOk(Tag tag, ImapCommand command, HumanReadableText displayTextKey, ResponseCode code) {
        return createResponse(StatusResponse.Type.OK, tag, command, displayTextKey, code);
    }

    @Override
    public StatusResponse taggedOk(Tag tag, ImapCommand command, HumanReadableText displayTextKey) {
        return taggedOk(tag, command, displayTextKey, null);
    }

    @Override
    public StatusResponse untaggedBad(HumanReadableText displayTextKey, ResponseCode code) {
        return taggedBad(null, null, displayTextKey, code);
    }

    @Override
    public StatusResponse untaggedBad(HumanReadableText displayTextKey) {
        return untaggedBad(displayTextKey, null);
    }

    @Override
    public StatusResponse untaggedNo(HumanReadableText displayTextKey, ResponseCode code) {
        return taggedNo(null, null, displayTextKey, code);
    }

    @Override
    public StatusResponse untaggedNo(HumanReadableText displayTextKey) {
        return untaggedNo(displayTextKey, null);
    }

    @Override
    public StatusResponse untaggedOk(HumanReadableText displayTextKey, ResponseCode code) {
        return taggedOk(null, null, displayTextKey, code);
    }

    @Override
    public StatusResponse untaggedOk(HumanReadableText displayTextKey) {
        return untaggedOk(displayTextKey, null);
    }
}