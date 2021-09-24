__kernel
void space_kernel(float delta,
                    int total,
                    __global float *mass,
                    __global float3 *pos,
                    __global float3 *vel
                  )
{
    int index = get_global_id(0);
    float3 p = pos[index];
    float3 v = vel[index];
#ifdef DEBUG
    printf("index: %d, pos: %f, %f, %f, vel: %f, %f, %f\n", index, p.x, p.y, p.z, v.x, v.y, v.z);
#endif
    // apply forces kernel
    float3 sum = (float3)(0.); // -> G * sum = F, a = F / m

    for ( size_t i = 0; i < total; i++ ) {
        if ( i == index ) continue;
        sum += mass[i] / pow(p - pos[i], 2);

    }

    pos[index] += v;
}

