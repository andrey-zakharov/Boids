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

/// origin of ant, cp - current point, np - next point to check
bool check_valid(const float2 origin, const float2 vel, const float2 np) {
    const float min_dot = cos(radians(23.));
    const float nextlen = length( np - origin );

	if ( dot( normalize( vel ), normalize( np - origin ) ) < min_dot ) return false;
    if ( nextlen > length(vel) ) return false;
    return true;
}

__constant float2 d[8] = {
	(float2)(-1., -1.), (float2)(0., -1.), (float2)(1., -1.),
	(float2)(-1.,  0.), 			   (float2)(1.,  0.),
	(float2)(-1., +1.), (float2)(0., +1.), (float2)(1., +1.),
};

__constant const size_t StateEmpty = 0;
__constant const size_t StateFull = 0;
enum State { empty = 0, full = 1 } ;
enum CellType { cellEmpty = 0, nest = 1, food = 2, obstacle = 3 } ;

__kernel
void ant_kernel(
    float delta,
    float max_speed,
    uint w,
    uint h,
    __global __read_only uchar*     ground,
    __global float*     pheromones,
    __global float2*    coords,
    __global float2*    velocities,
    __global uchar*     state
    //__global float2*    out
)
{
    float accel = max_speed / 10.;

    int index = get_global_id(0);
    // test 1
    // coords[index] = wrap_bounds(coords[index] + (float2)(1.0, 1.0), w, h);
    //velocities[index] = velocities[index]  + max_speed;
    //    return;
    float2 pos = coords[index];
    float2 vel = velocities[index];
    uchar out_state = state[index];
    bool emp = state[index] == 0;

    // random fluctuation
    float r = (random(pos) - 0.5) * 0.17453292519943 /*10 deg*/;
    //r = 0.01;
    vel = (float2)( vel.x * cos(r) - vel.y * sin(r), vel.x * sin(r) + vel.y * cos(r) );

    queue cells;
    queue_init(&cells);
    queue_push(&cells, convert_uint2(pos));
    uint2 origin = cells.queue[0];
    // and center of current cell
    float2 iorigin = convert_float2(origin) + (float2)0.5;
    size_t obstacles_found = 0;

    /// BFS
    while(!queue_empty(&cells)) {
        uint2 cp = queue_pop(&cells);
        float2 fcp = convert_float2(cp) + (float2)0.5;
        
        // addition of this point
        if ( cp.x != origin.x && cp.y != origin.y ) {

        	if ( ground[ cp.x + cp.y * w ] == obstacle ) {
                obstacles_found++;
        		vel += convert_float2(origin - cp) * (float2)(delta * 2.0);
        		//vel = (float2)(0., 0.);
        	}
            /*if ( pheromones[ cp.x + cp.y * w ] > 0 ) {
                vel += 3.f / vel - convert_float2(cp - origin);
            }*/
        }

        // neighbours
        // first check by min angle, but at least get one with max dot product
        size_t added = 0;
        float maxdot = -100.;
        uint maxdoti = -1;
        for ( uint i = 0; i < sizeof(d)/sizeof(int2); i++ ) {
        	float2 fupp = fcp + d[i];
            uint2 upp = convert_uint2(fupp);

            if (check_valid(iorigin, vel, fupp) && !check_queued(&cells, upp)) {
            	// debug
            	uint2 debug = convert_uint2(wrap_bounds(fupp, w, h));
            	pheromones[(int)debug.x + (int)debug.y*w] = -1.0f;
                queue_push(&cells, upp);
                added ++;
            }

            float d = dot( normalize( vel ), normalize( fupp - iorigin ) );
            if ( d > maxdot ) {
                maxdot = d;
                maxdoti = i;
            }
        }

        /*if ( added == 0 ) {
            uint2 last_chance = convert_uint2(fcp + d[maxdoti]);
            if ( length (last_change - fcp) < length(vel) ) {
                pheromones[(int)last_chance.x + (int)last_chance.y * w] = -1.0f;
                queue_push(&cells, last_chance);
            }
        }*/


    }

    out_state = obstacles_found; // ? obstacle : empty;

    pheromones[(int)pos.x + (int)pos.y*w] = 1.0f;

    float len = length(vel);
    if (len == 0) {
        // add random length rotated vector
        float2 r = (float2)(0., accel);
        float a = random(pos+vel) * M_PI * 2;
        r = (float2)( r.y * sin(a), r.y * cos(a));
        vel += r;
    } else if (len < max_speed) {
        // accelerate
        vel += normalize(vel) * accel;
    } else if (len > max_speed) {
        vel = normalize(vel) * max_speed;
    }

    velocities[index] = vel;
    coords[index] = wrap_bounds(pos + velocities[index] * delta, w, h);
    state[index] = out_state;
}
