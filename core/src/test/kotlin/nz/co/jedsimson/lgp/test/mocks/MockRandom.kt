package nz.co.jedsimson.lgp.test.mocks

import kotlin.random.Random

class MockRandom : Random() {
    override fun nextBits(bitCount: Int): Int {
        TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
    }

    override fun nextDouble(): Double {
        return super.nextDouble()
    }
}