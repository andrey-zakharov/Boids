
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
#ifdef DEBUG
    printf("queue pushing (%d, %d)\n", v.x, v.y);
#endif
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

void queue_print( queue const * q ) {
    printf("queue size: %d (front - %d)\n", q->pos_tail - q->pos_front, q->pos_front);
    for( size_t p = 0; p < q->pos_tail; p++ ) {
        printf(" %d - (%u, %u)", p, q->queue[p].x, q->queue[p].y);
    }
    printf("\n");
}
