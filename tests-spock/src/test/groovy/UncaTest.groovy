/*
 * Copyright 2020 EPAM Systems
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
import spock.lang.Specification

class UncaTest extends Specification {

//    def messageService = new MessageService()

    def 'Should not be run'() {
        expect: 'Should return the correct message'
        println 'Should not be run'
//        messageService.getMessage() == 'Hello World!'
    }
}