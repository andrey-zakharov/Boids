package me.zakharov

import org.lwjgl.glfw.GLFW.*
import org.lwjgl.glfw.GLFWErrorCallback
import org.lwjgl.glfw.GLFWKeyCallback
import org.lwjgl.opengl.GL
import org.lwjgl.opengl.GL11.*
import org.lwjgl.system.MemoryUtil.NULL


class MainWindow(val width: Int = 800, val height: Int = 600) {
    private var sp = 0.0f
    private var swapcolor: Boolean = false
    private var errorCallback : GLFWErrorCallback? = null
    private var keyCallback : GLFWKeyCallback? = null

    private var window : Long? = null

    private fun init() {

        // Setup an error callback. The default implementation
        // will print the error message in System.err.
        errorCallback = glfwSetErrorCallback(GLFWErrorCallback.createPrint(System.err))

        // Initialize GLFW. Most GLFW functions will not work before doing this.
        if ( !glfwInit() ) {
            throw IllegalStateException("Unable to initialize GLFW")
        }

        // Configure our window
        glfwDefaultWindowHints()
        glfwWindowHint(GLFW_VISIBLE, GLFW_FALSE)
        glfwWindowHint(GLFW_RESIZABLE, GLFW_TRUE)

        // Create the window
        window = glfwCreateWindow(width, height, "Hello World!", NULL, NULL)
        if (window == NULL) {
            throw RuntimeException("Failed to create the GLFW window")
        }

        // Setup a key callback. It will be called every time a key is pressed, repeated or released.
        keyCallback = glfwSetKeyCallback(window!!, object : GLFWKeyCallback() {
            override fun invoke(window: kotlin.Long,
                                key: kotlin.Int,
                                scancode: kotlin.Int,
                                action: kotlin.Int,
                                mods: kotlin.Int) {

                if (key == GLFW_KEY_ESCAPE && action == GLFW_RELEASE ) {
                    glfwSetWindowShouldClose(window, true)
                }

            }
        })

        // Get the resolution of the primary monitor
        val vidmode = glfwGetVideoMode(glfwGetPrimaryMonitor())!!

        // Center our window
        glfwSetWindowPos(
                window!!,
                (vidmode.width() - width) / 2,
                (vidmode.height() - height) / 2
        );

        // Make the OpenGL context current
        glfwMakeContextCurrent(window!!)
        // Enable v-sync
        glfwSwapInterval(1)

        // Make the window visible
        glfwShowWindow(window!!)

    }

    private fun loop() {
        GL.createCapabilities()

        //glClearColor(1.0f, 0.0f, 0.0f, 0.0f);

        // Run the rendering loop until the user has attempted to close
        // the window or has pressed the ESCAPE key.
        while (!glfwWindowShouldClose(window!!)) {
            if (!swapcolor) {
                glClearColor(0.0f, 0.0f, 1.0f, 0.0f);
            } else {
                glClearColor(0.0f, 1.0f, 0.0f, 0.0f);
            }
            // Clear the framebuffer
            glClear(GL_COLOR_BUFFER_BIT or GL_DEPTH_BUFFER_BIT)
            update()
            render()
            // Swap the color buffers
            glfwSwapBuffers(window!!)

            // Poll for window events. The key callback above will only be
            // invoked during this call.
            glfwPollEvents()

        }

    }

    private fun update() {
        sp += 0.01f
        if (sp > 1.0f) {
            sp = 0.0f
            swapcolor = !swapcolor
        }
    }

    private  fun render() {
        drawQuad()
    }
    private fun drawQuad() {
        if (!swapcolor) {
            glColor3f(0.0f, 1.0f, 0.0f)
        } else {
            glColor3f(0.0f, 0.0f, 1.0f)
        }
        glBegin(GL_QUADS)
        run {
            glVertex3f(-sp, -sp, 0.0f)
            glVertex3f(sp, -sp, 0.0f)
            glVertex3f(sp, sp, 0.0f)
            glVertex3f(-sp, sp, 0.0f)
        }
        glEnd()
    }

    fun run() {

        try {

            init()
            loop()
            // Destroy window
            glfwDestroyWindow(window!!);
            keyCallback?.free()

        } finally {

            // Terminate GLFW
            glfwTerminate()
            errorCallback?.free()

        }
    }

}