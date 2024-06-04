/**
 * Copyright 2020 - 2022 EPAM Systems
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
package com.epam.drill.test.agent.request

import com.epam.drill.common.agent.request.HeadersRetriever

actual object HeadersRetriever : HeadersRetriever {
    actual external override fun adminAddressHeader(): String
    actual external override fun adminAddressValue(): String
    actual external override fun sessionHeader(): String
    actual external override fun agentIdHeader(): String
    actual external override fun agentIdHeaderValue(): String
}