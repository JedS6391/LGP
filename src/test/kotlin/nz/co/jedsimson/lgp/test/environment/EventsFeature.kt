package nz.co.jedsimson.lgp.test.environment

import com.nhaarman.mockitokotlin2.any
import com.nhaarman.mockitokotlin2.mock
import com.nhaarman.mockitokotlin2.times
import com.nhaarman.mockitokotlin2.verify
import nz.co.jedsimson.lgp.core.environment.events.Event
import nz.co.jedsimson.lgp.core.environment.events.EventDispatcher
import nz.co.jedsimson.lgp.core.environment.events.EventListener
import org.spekframework.spek2.Spek
import org.spekframework.spek2.style.gherkin.Feature

object EventsFeature : Spek({
    Feature("Events") {

        Scenario("Event listeners can be registered") {

            Given("An event listener is registered") {
                EventDispatcher.register(object : EventListener<TestEvent> {
                    override fun handle(event: TestEvent) {
                        println(event.identifier)
                    }
                })
            }

            Then("The event dispatcher has a single event listener registered") {
                val numberOfListeners = EventDispatcher.numberOfListeners

                assert(numberOfListeners == 1) { "Expected 1 listener but got $numberOfListeners"}
            }

            Given("An event listener is registered") {
                EventDispatcher.register(object : EventListener<TestEvent> {
                    override fun handle(event: TestEvent) {
                        println(event.identifier)
                    }
                })
            }

            Then("The event dispatcher has two event listeners registered") {
                val numberOfListeners = EventDispatcher.numberOfListeners

                assert(numberOfListeners == 2) { "Expected 2 listeners but got $numberOfListeners"}
            }
        }

        Scenario("An event can be dispatched to a listener") {
            val listener = mock<EventListener<TestEvent>>()

            Given("An event listener is registered") {
                EventDispatcher.register(listener)
            }

            When("An event that the listener is not listening for is dispatched") {
                EventDispatcher.dispatch(SecondaryTestEvent())
            }

            Then("The listener is not called") {
                verify(listener, times(0)).handle(any())
            }

            When("An event that the listener is listening for is dispatched") {
                EventDispatcher.dispatch(TestEvent())
            }

            Then("The listener is called") {
                verify(listener, times(1)).handle(any())
            }
        }

        Scenario("An event can be dispatched to multiple listeners") {
            val listener1 = mock<EventListener<TestEvent>>()
            val listener2 = mock<EventListener<TestEvent>>()
            val listener3 = mock<EventListener<SecondaryTestEvent>>()

            Given("Multiple event listeners are registered") {
                EventDispatcher.register(listener1)
                EventDispatcher.register(listener2)
                EventDispatcher.register(listener3)
            }

            When("An event that the listeners are listening for is dispatched") {
                EventDispatcher.dispatch(TestEvent())
            }

            Then("The listeners are called") {
                verify(listener1, times(1)).handle(any())
                verify(listener2, times(1)).handle(any())
                verify(listener3, times(0)).handle(any())
            }
        }

        Scenario("Different event are dispatched to different listeners") {
            val listener1 = mock<EventListener<TestEvent>>()
            val listener2 = mock<EventListener<SecondaryTestEvent>>()

            Given("Multiple event listeners are registered") {
                EventDispatcher.register(listener1)
                EventDispatcher.register(listener2)
            }

            When("An event that each listener is listening for is dispatched") {
                EventDispatcher.dispatch(TestEvent())
                EventDispatcher.dispatch(SecondaryTestEvent())
            }

            Then("The listeners are called") {
                verify(listener1, times(1)).handle(any())
                verify(listener2, times(1)).handle(any())
            }
        }
    }
})

class TestEvent : Event()

class SecondaryTestEvent : Event()