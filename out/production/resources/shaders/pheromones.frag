#ifdef GL_ES
    precision mediump float;
#endif

varying vec4 v_color;
varying vec2 v_texCoords;
uniform sampler2D u_texture;
uniform mat4 u_projTrans;

void main() {
        float pher = float( texture2D(u_texture, v_texCoords) );
        vec4 color = vec4(0.0, 0.0, 0.0, 0.0);
        if (pher < 0) {
            color = vec4(0, 1., 0, -pher);
        } else if ( pher > 0 ) {
            color = vec4(0, 0, 1., pher);
        }

        gl_FragColor = color;
}