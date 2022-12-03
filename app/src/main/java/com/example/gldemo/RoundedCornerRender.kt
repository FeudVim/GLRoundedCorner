package com.example.gldemo

import android.opengl.GLES30.*
import android.opengl.GLSurfaceView
import java.nio.ByteBuffer
import java.nio.ByteOrder
import javax.microedition.khronos.egl.EGLConfig
import javax.microedition.khronos.opengles.GL10
import kotlin.math.cos
import kotlin.math.sin

class RoundedCornerRender(private val radius: Float = 0.5f, private val precision: Int = 256) : GLSurfaceView.Renderer {

    private var inColor = -1
    private var program = 0
    private val vao = intArrayOf(0, 0)
    private val vbo = intArrayOf(0, 0)
    private val ebo = intArrayOf(0, 0)

    private val rectLeftBottom = floatArrayOf(-1f, -1f, 0.5f)
    private val rectRightBottom = floatArrayOf(1f, -1f, 0.5f)
    private val rectRightTop = floatArrayOf(1f, 1f, 0.5f)
    private val rectLeftTop = floatArrayOf(-1f, 1f, 0.5f)

    private val elementDrawIndex = intArrayOf(0, 1, 2, 0, 2, 3)
    private val vertex =
        ByteBuffer.allocateDirect(Float.SIZE_BYTES * (rectLeftBottom.size + rectRightBottom.size + rectRightTop.size + rectLeftTop.size))
            .order(ByteOrder.nativeOrder())
            .asFloatBuffer().put(rectLeftBottom).put(rectRightBottom).put(rectRightTop).put(rectLeftTop).position(0)
    private val index =
        ByteBuffer.allocateDirect(Int.SIZE_BYTES * elementDrawIndex.size)
            .order(ByteOrder.nativeOrder())
            .asIntBuffer().put(elementDrawIndex).position(0)


    private val cullAreaVertex = Array(4) { FloatArray((precision + 1) * 3) }
    private val cullAreaElementDrawIndex = Array(4) { IntArray(precision * 3) }
    private val cullAreaVertexBuffer =
        ByteBuffer.allocateDirect(cullAreaVertex.sumOf { it.size } * Float.SIZE_BYTES)
            .order(ByteOrder.nativeOrder()).asFloatBuffer()
    private val cullAreaElementDrawIndexBuffer =
        ByteBuffer.allocateDirect(cullAreaElementDrawIndex.sumOf { it.size } * Int.SIZE_BYTES)
            .order(ByteOrder.nativeOrder()).asIntBuffer()

    init {
        generateCutoutAreaVertex(rectLeftBottom)
        generateCutoutAreaVertex(rectRightBottom)
        generateCutoutAreaVertex(rectRightTop)
        generateCutoutAreaVertex(rectLeftTop)
        cullAreaVertexBuffer.position(0)
        cullAreaElementDrawIndexBuffer.position(0)
    }


    private fun generateCutoutAreaVertex(rectVertex: FloatArray) {

        val p = cullAreaVertexBuffer.position() / 3
        cullAreaVertexBuffer.put(rectVertex)
        for (i in 0 until precision) {
            val angle = (Math.PI.toFloat() / 2 / precision) * i
            val y = sin(angle) * radius
            val x = cos(angle) * radius


            val originX = rectVertex[0] + radius * if (rectVertex[0] > 0) -1 else 1
            val originY = rectVertex[1] + radius * if (rectVertex[1] > 0) -1 else 1

            cullAreaVertexBuffer.put(originX + x * if (rectVertex[0] > 0) 1 else -1)
            cullAreaVertexBuffer.put(originY + y * if (rectVertex[1] > 0) 1 else -1)
            cullAreaVertexBuffer.put(0f)
            if (i != 0) {
                cullAreaElementDrawIndexBuffer.put(p)
                    .put(p + i - 1)
                    .put(p + i)
            }
        }
    }


    override fun onSurfaceCreated(gl: GL10?, config: EGLConfig?) {
        glEnable(GL_STENCIL_TEST)
        glEnable(GL_DEPTH_TEST)
        glGenVertexArrays(2, vao, 0)
        glGenBuffers(2, vbo, 0)
        glGenBuffers(2, ebo, 0)

        glBindVertexArray(vao[0])
        glBindBuffer(GL_ARRAY_BUFFER, vbo[0])
        glBufferData(GL_ARRAY_BUFFER, cullAreaVertexBuffer.capacity() * Int.SIZE_BYTES, cullAreaVertexBuffer, GL_STATIC_DRAW)
        glBindBuffer(GL_ELEMENT_ARRAY_BUFFER, ebo[0])
        glBufferData(GL_ELEMENT_ARRAY_BUFFER, Int.SIZE_BYTES * cullAreaElementDrawIndexBuffer.capacity(), cullAreaElementDrawIndexBuffer, GL_STATIC_DRAW)
        glVertexAttribPointer(0, 3, GL_FLOAT, false, 3 * Int.SIZE_BYTES, 0)
        glEnableVertexAttribArray(0)

        glBindVertexArray(vao[1])
        glBindBuffer(GL_ARRAY_BUFFER, vbo[1])
        glBufferData(GL_ARRAY_BUFFER, vertex.capacity() * Float.SIZE_BYTES, vertex, GL_STATIC_DRAW)
        glBindBuffer(GL_ELEMENT_ARRAY_BUFFER, ebo[1])
        glBufferData(GL_ELEMENT_ARRAY_BUFFER, index.capacity() * Int.SIZE_BYTES, index, GL_STATIC_DRAW)
        glVertexAttribPointer(0, 3, GL_FLOAT, false, 3 * Int.SIZE_BYTES, 0)
        glEnableVertexAttribArray(0)
        glBindVertexArray(0)
        glDisableVertexAttribArray(0)

        val vertexShader = glCreateShader(GL_VERTEX_SHADER)
        glShaderSource(
            vertexShader, "#version 300 es\n" +
                    "layout(location=0) in vec3 aPos;\n" +
                    "out vec3 color;\n" +
                    "void main(){\n" +
                    "gl_Position=vec4(aPos.x,aPos.y,aPos.z,1.0f);\n" +
                    "}\n"
        )
        glCompileShader(vertexShader)
        val fragmentShader = glCreateShader(GL_FRAGMENT_SHADER)
        glShaderSource(
            fragmentShader, "#version 300 es\n" +
                    "precision mediump float;" +
                    "out vec4 outColor;\n" +
                    "uniform vec4 inColor;\n" +
                    "void main(){\n" +
                    "outColor=inColor;\n" +
                    "}\n"
        )
        glCompileShader(fragmentShader)
        program = glCreateProgram()
        glAttachShader(program, vertexShader)
        glAttachShader(program, fragmentShader)
        glLinkProgram(program)
        glDeleteShader(vertexShader)
        glDeleteShader(fragmentShader)
        inColor = glGetUniformLocation(program, "inColor")
    }

    override fun onSurfaceChanged(gl: GL10?, width: Int, height: Int) {
        glViewport(width / 4, height / 4, width / 2, height / 2)
    }

    override fun onDrawFrame(gl: GL10?) {
        glClear(GL_COLOR_BUFFER_BIT or GL_DEPTH_BUFFER_BIT or GL_STENCIL_BUFFER_BIT)
        glClearColor(1f, 1f, 1f, 1f)
        glUseProgram(program)
        glStencilMask(0XFF)
        glStencilFunc(GL_NEVER, 0x1, 0xff)
        glStencilOp(GL_REPLACE, GL_KEEP, GL_KEEP)
        glBindVertexArray(vao[0])
        glUniform4f(inColor, 1.0f, 0f, 0f, 1f)
        glDrawElements(GL_TRIANGLES, cullAreaElementDrawIndexBuffer.capacity(), GL_UNSIGNED_INT, 0)
        glStencilMask(0x00)
        glStencilFunc(GL_NOTEQUAL, 0x01, 0xff)
        glStencilOp(GL_KEEP, GL_KEEP, GL_KEEP)
        glBindVertexArray(vao[1])
        glUniform4f(inColor, 1.0f, 1.0f, 0f, 1f)
        glDrawElements(GL_TRIANGLES, index.capacity(), GL_UNSIGNED_INT, 0)
        glUseProgram(0)
    }

}
