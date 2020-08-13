import spock.lang.Specification

class UncaTest extends Specification {

//    def messageService = new MessageService()

    def 'Should not be run'() {
        expect: 'Should return the correct message'
        println 'Should not be run'
//        messageService.getMessage() == 'Hello World!'
    }
}