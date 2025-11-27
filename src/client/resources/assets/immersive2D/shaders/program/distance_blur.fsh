#version 330 compatibility

uniform sampler2D DiffuseSampler;
uniform sampler2D DepthSampler;

uniform mat4 InverseTransformMatrix;
uniform mat4 ModelViewMat;
uniform vec3 CameraPos;

uniform vec3 PlaneOffset;
uniform float PlaneSlope;

in vec2 texCoord;
in vec2 oneTexel;

out vec4 fragColor;

// Most of this is just a direct copy of fog.fsh, but there isn't any #include and I don't think I can connect the outputs, so it is what it is

// almost the same as in plane
float sdf(vec3 point) {
    float x = (PlaneSlope * PlaneOffset.x - PlaneOffset.z + point.x / PlaneSlope + point.z) / (PlaneSlope + 1. / PlaneSlope);
    float z = PlaneSlope * (x - PlaneOffset.x) + PlaneOffset.z;

    vec3 to_point = vec3(point.x - x, 0, point.z - z);

    // we can assume it's always positive as negative would be culled
    return length(to_point);
}

void main() {
    float sceneDepth = texture(DepthSampler, texCoord).x;

    if (sceneDepth == 1) {
        fragColor = texture(DiffuseSampler, texCoord);
        return;
    }

    vec3 ndc = vec3(texCoord.xy, sceneDepth) * 2.0 - 1.0;
    vec4 homPos = InverseTransformMatrix * vec4(ndc, 1.0);
    vec3 viewPos = homPos.xyz / homPos.w;

    vec3 worldPos = (inverse(ModelViewMat) * vec4(viewPos, 1.)).xyz + CameraPos;
    float dist = sdf(worldPos);

    float radius = step(1.6, dist) + step(3.6, dist);

    vec3 finalCol = vec3(0.);
    for (float x = -radius; x <= radius; x++) {
        for (float y = -radius; y <= radius; y++) {
            finalCol += texture(DiffuseSampler, texCoord + vec2(x, y) * oneTexel).rgb;
        }
    }

    fragColor = vec4(finalCol/pow(2. * radius + 1., 2.), 1.);
}