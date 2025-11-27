#version 330 compatibility

uniform sampler2D DiffuseSampler;

uniform vec3 CameraPos;
uniform vec3 PlayerPos;
uniform vec2 LightLevel;

in vec2 texCoord;

out vec4 fragColor;

void main() {
    vec4 tex = texture(DiffuseSampler, texCoord);

    vec3 vignetteColor = mix(vec3(0.), vec3(0.878, 0.396, 0.247), pow(LightLevel.r/15., 4.));
    float vignetteFactor = 4. * (1. - clamp(max(LightLevel.r/1.05, LightLevel.g)/15., 0., 1.));

    fragColor = mix(tex, vec4(vignetteColor, 0.), clamp(distance(vec2(0.5), texCoord) * vignetteFactor - 0.2, 0., 1.));
}