// Package chat is the reference solution for Module 4.8.
// Try the assignment yourself before reading this!
package chat

type Message struct {
	Room string
	From string
	Body string
	Seq  uint64
}

type Client struct {
	ID  string
	Out chan Message
}

func NewClient(id string, buffer int) *Client {
	return &Client{ID: id, Out: make(chan Message, buffer)}
}

type Hub struct {
	joinCh  chan joinCmd
	leaveCh chan leaveCmd
	bcastCh chan bcastCmd
	stopCh  chan struct{}
	doneCh  chan struct{}

	rooms map[string]map[*Client]struct{}
	seq   map[string]uint64
}

type joinCmd struct {
	room string
	c    *Client
}

type leaveCmd struct {
	room string
	c    *Client
}

type bcastCmd struct {
	msg Message
}

func NewHub() *Hub {
	return &Hub{
		joinCh:  make(chan joinCmd),
		leaveCh: make(chan leaveCmd),
		bcastCh: make(chan bcastCmd),
		stopCh:  make(chan struct{}),
		doneCh:  make(chan struct{}),
		rooms:   make(map[string]map[*Client]struct{}),
		seq:     make(map[string]uint64),
	}
}

// Run is the single goroutine that owns all Hub state. No locks needed: the only
// goroutine reading/writing h.rooms and h.seq is this one.
func (h *Hub) Run() {
	for {
		select {
		case cmd := <-h.joinCh:
			members := h.rooms[cmd.room]
			if members == nil {
				members = make(map[*Client]struct{})
				h.rooms[cmd.room] = members
			}
			members[cmd.c] = struct{}{}

		case cmd := <-h.leaveCh:
			if members := h.rooms[cmd.room]; members != nil {
				delete(members, cmd.c)
				if len(members) == 0 {
					delete(h.rooms, cmd.room)
				}
			}

		case cmd := <-h.bcastCh:
			room := cmd.msg.Room
			h.seq[room]++
			cmd.msg.Seq = h.seq[room]
			for c := range h.rooms[room] {
				// Non-blocking send: a slow client must not freeze the Hub.
				// A real server would mark this client for disconnect here.
				select {
				case c.Out <- cmd.msg:
				default:
				}
			}

		case <-h.stopCh:
			close(h.doneCh)
			return
		}
	}
}

func (h *Hub) Join(room string, c *Client) {
	h.joinCh <- joinCmd{room: room, c: c}
}

func (h *Hub) Leave(room string, c *Client) {
	h.leaveCh <- leaveCmd{room: room, c: c}
}

func (h *Hub) Broadcast(room string, msg Message) {
	msg.Room = room
	h.bcastCh <- bcastCmd{msg: msg}
}

func (h *Hub) Stop() {
	close(h.stopCh)
	<-h.doneCh
}
