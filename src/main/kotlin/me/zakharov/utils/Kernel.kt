package me.zakharov.utils

import me.apemanzilla.ktcl.CLKernel
import me.apemanzilla.ktcl.cl10.*
import me.zakharov.utils.IHeadlessActor
import java.io.ByteArrayOutputStream
import java.io.PrintStream

/// in sources (${kernel_name}.c) should be kernel function name $kernel_name
open class Kernel(val kernelName: String,
                  val kernelSource: String = Kernel::class.java.getResource("/kernels/${kernelName}.c")!!.readText(),
                  val kernelDefines: Array<String> = arrayOf(), ///< additional defines to CL build
                  kernelInitArgs: CLKernel.() -> Unit = { }
) : IHeadlessActor {
    // cl stuff
    companion object {
        private var i = 0
        val clDevice by lazy {

            val out = ByteArrayOutputStream()
            with(PrintStream(out)) {
                getPlatforms().forEach {
                    println("platform: ${it.name} vendor: ${it.vendor} version: ${it.version}")
                    it.getDevices().forEach {
                        println("\tdevice: ${it.name} vendor: ${it.vendor} version: ${it.version} ${it.execCapabilities} extension: ${it.extensions}")
                    }
                    try {
                        println("\tGPU: ${it.getDevices(DeviceType.GPU)}")
                    } catch (e: Throwable) {}
                    try {
                        println("\tCPU: ${it.getDevices(DeviceType.CPU)}")
                    } catch (e: Throwable) {}
                }
            }
            println(out.toString())

            val device = getPlatforms()
                .first { clPlatform -> //clPlatform.getDefaultDevice() != null
                    try {
                        clPlatform.getDevices(DeviceType.GPU).isNotEmpty()
                    } catch(e: Throwable) {false}
                }
                .getDevices(type = DeviceType.GPU).first()
            println("Using: $device")
            device
        }
        val ctx by lazy { clDevice.createContext() }
        val cmd by lazy {
            println("creating CMD QUEUE ${i++}")
            clDevice.createCommandQueue(ctx)
        }
    }
    internal val kernel by lazy {
        ctx.createProgramWithSource(kernelSource).also {
            it.build(kernelDefines.map { "-D$it" }.joinToString(" ") + " -cl-std=CL2.0")
        }
            .createKernel(kernelName).apply(kernelInitArgs)
    }
    // TBD auto loading buffers
    override fun act(delta: Float) = Unit
}