package me.zakharov.space.model

import com.badlogic.gdx.math.Vector3

data class Body(
    val mass: Float,
    val pos: Vector3,
    val vel: Vector3,
) {
    // accum to other mass
    fun accum(m: Float, pos: Vector3) {
        // Sum(mv2) = Sum(mv2)
        // v = sqrt(v2 * m2/m1)
        // mass += m
        // vel = vel.len2() * m / mass
    }

    fun update(delta: Float) {

    }

}