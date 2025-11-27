#version 330 compatibility

in vec4 Position;

uniform mat4 ProjMat;
uniform mat4 ModelViewMat;
uniform vec2 OutSize;

out vec2 texCoord;
out vec2 oneTexel;

void main() {
    vec4 outPos = ProjMat * vec4(Position.xy, 0.0, 1.0);
    gl_Position = vec4(outPos.xy, 0.2, 1.0);

    oneTexel = 1. / OutSize;
    texCoord = Position.xy / OutSize;
}
