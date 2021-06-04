typedef struct {
    uint pos_tail; // push here
    uint pos_front; // pop here
    uint2 queue[MAX_QUEUE_SIZE];
} queue;

void queue_init(queue *q) {
	q->pos_tail = 0;
	q->pos_front = 0;
}

bool queue_empty(queue const *q) {
    return q->pos_tail <= q->pos_front;
}

void queue_push(queue* q, uint2 v) {
	if (q->pos_tail >= MAX_QUEUE_SIZE ) {
		return;
	}
    q->queue[q->pos_tail++] = v;
}

uint2 queue_pop(queue* q) {
    return q->queue[q->pos_front++];
}

bool check_queued( queue const * q, const uint2 p) {
	for( size_t i = 0; i < q->pos_tail; i++) {
		//if ( all(q->queue[i] == p) ) return true; <-- crashed video driver
		if ( q->queue[i].x == p.x && q->queue[i].y == p.y ) return true;
	}
	return false;
}

float random (float2 _st) {
    float rem;
    return fract(sin(dot(_st.xy, (float2)(12.9898,78.233))) * 43758.5453123, &rem);
}

float2 wrap_bounds(float2 pos, uint w, uint h) {
    float2 ret = pos;
    if (ret.x < 0 ) ret.x += w;
    else if (ret.x > w ) ret.x -= w;
    if (ret.y < 0) ret.y += h;
    else if ( ret.y > h ) ret.y -= h;
    return ret;
}

__constant const float2 corners[4] = {
	(float2)(0., 0.),
	(float2)(1. - FLT_EPSILON, 0.),
	(float2)(0., 1. - FLT_EPSILON),
	(float2)(1. - FLT_EPSILON, 1. - FLT_EPSILON)
};

/// origin of ant, cp - current point, np - next point to check
bool check_valid(const float2 origin, const float2 vel, const uint2 np) {
    const float min_dot = cos(radians(10.));
    const float2 fnp = convert_float2(np) + (float2)0.5;
    const float nextlen = length( fnp - origin );

	if ( dot( normalize( vel ), normalize( fnp - origin ) ) < min_dot ) return false;
    if ( nextlen > length(vel) ) return false;
    return true;
}

__constant int2 d[8] = {
	(int2)(-1, -1), (int2)(0, -1), (int2)(1, -1),
	(int2)(-1,  0), 			   (int2)(1,  0), 
	(int2)(-1, +1), (int2)(0, +1), (int2)(1, +1),
};

__kernel
void ant_kernel(
    float delta,
    float max_speed,
    uint w,
    uint h,
    __global __read_only uchar* ground,
    __global float *pheromones,
    __global float2 *coords,
    __global float2 *velocities,
    __global float2 *out,
    __global float *outPheromones
    )
{
    int index = get_global_id(0);

    float2 pos = coords[index];
    float2 vel = velocities[index];
    // random fluctuation
    float r = (random(pos) - 0.5) * 0.17453292519943 /*10 deg*/;
    r = 0.01;
    vel = (float2)( vel.x * cos(r) - vel.y * sin(r), vel.x * sin(r) + vel.y * cos(r) );

    queue cells;
    queue_init(&cells);
    queue_push(&cells, convert_uint2(pos));
    uint2 origin = cells.queue[0];

    while(!queue_empty(&cells)) {
        uint2 cp = queue_pop(&cells);
        
        // addition of this point
        if ( cp.x != origin.x && cp.y != origin.y ) {
        	if ( ground[ cp.x + cp.y * w] == 3 ) {
        		vel += convert_float2(origin - cp);
        		//vel = (float2)(0., 0.);
        	}
            if ( pheromones[ cp.x + cp.y * w ] > 0 ) {
                vel += 3.f / vel - convert_float2(cp - origin);
            }
        }

        // neibours
        for ( uint i = 0; i < sizeof(d)/sizeof(int2); i++ ) {
        	float2 fupp = convert_float2(convert_int2(cp) + d[i]) + (float2)(0.5f, 0.5f);
            uint2 upp = convert_uint2(wrap_bounds(fupp, w, h));
            
            if (check_valid(pos, vel, upp) && !check_queued(&cells, upp)) {
            	pheromones[(int)upp.x + (int)upp.y*w] = -1.0f;
                queue_push(&cells, upp);
            }
        }
    }

    pheromones[(int)pos.x + (int)pos.y*w] = 1.0f;


    velocities[index] = normalize(vel) * max_speed;

    out[index] = wrap_bounds(pos + velocities[index] * delta, w, h);
}
