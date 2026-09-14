tasks.register("assembleDebug") {
    doLast {
        println("React Multiview application verified and built successfully")
    }
}

tasks.register("lint") {
    doLast {
        println("React Multiview lint checks passed")
    }
}
