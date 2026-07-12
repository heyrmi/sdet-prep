import type {
	AuthUser,
	Book,
	FreeApiResponse,
	Joke,
	LoginResponse,
	PaginatedData,
	Product,
	RandomUser,
	RegisterResponse,
	Stock,
	Todo,
} from '@/types/freeapi';
import { ApiClient } from './ApiClient';

type PaginationParams = { page?: number; limit?: number; query?: string };

export class FreeApiClient extends ApiClient {
	// ── Healthcheck ──────────────────────────────────────────────

	async healthcheck() {
		return this.get<FreeApiResponse<string>>('healthcheck');
	}

	// ── Random Users ─────────────────────────────────────────────

	async getUsers(params?: PaginationParams) {
		return this.get<FreeApiResponse<PaginatedData<RandomUser>>>(
			'public/randomusers',
			{ params: params as Record<string, string | number> },
		);
	}

	async getUserById(id: number) {
		return this.get<FreeApiResponse<RandomUser>>(`public/randomusers/${id}`, {
			failOnStatusCode: false,
		});
	}

	async getRandomUser() {
		return this.get<FreeApiResponse<RandomUser>>(
			'public/randomusers/user/random',
		);
	}

	// ── Random Products ──────────────────────────────────────────

	async getProducts(params?: PaginationParams) {
		return this.get<FreeApiResponse<PaginatedData<Product>>>(
			'public/randomproducts',
			{ params: params as Record<string, string | number> },
		);
	}

	async getProductById(id: number) {
		return this.get<FreeApiResponse<Product>>(`public/randomproducts/${id}`, {
			failOnStatusCode: false,
		});
	}

	async getRandomProduct() {
		return this.get<FreeApiResponse<Product>>(
			'public/randomproducts/product/random',
		);
	}

	// ── Random Jokes ─────────────────────────────────────────────

	async getJokes(params?: PaginationParams) {
		return this.get<FreeApiResponse<PaginatedData<Joke>>>(
			'public/randomjokes',
			{ params: params as Record<string, string | number> },
		);
	}

	async getJokeById(id: number) {
		return this.get<FreeApiResponse<Joke>>(`public/randomjokes/${id}`, {
			failOnStatusCode: false,
		});
	}

	async getRandomJoke() {
		return this.get<FreeApiResponse<Joke>>('public/randomjokes/joke/random');
	}

	// ── Books ────────────────────────────────────────────────────

	async getBooks(params?: PaginationParams) {
		return this.get<FreeApiResponse<PaginatedData<Book>>>('public/books', {
			params: params as Record<string, string | number>,
		});
	}

	async getBookById(id: number) {
		return this.get<FreeApiResponse<Book>>(`public/books/${id}`, {
			failOnStatusCode: false,
		});
	}

	async getRandomBook() {
		return this.get<FreeApiResponse<Book>>('public/books/book/random');
	}

	// ── Stocks ───────────────────────────────────────────────────

	async getStocks(params?: PaginationParams) {
		return this.get<FreeApiResponse<PaginatedData<Stock>>>('public/stocks', {
			params: params as Record<string, string | number>,
		});
	}

	// ── Auth ─────────────────────────────────────────────────────

	async register(email: string, username: string, password: string) {
		return this.post<FreeApiResponse<RegisterResponse>>('users/register', {
			data: { email, username, password },
			failOnStatusCode: false,
		});
	}

	async login(username: string, password: string) {
		return this.post<FreeApiResponse<LoginResponse>>('users/login', {
			data: { username, password },
			failOnStatusCode: false,
		});
	}

	async getCurrentUser() {
		return this.get<FreeApiResponse<AuthUser>>('users/current-user', {
			failOnStatusCode: false,
		});
	}

	async logout() {
		return this.post<FreeApiResponse<object>>('users/logout', {
			failOnStatusCode: false,
		});
	}

	// ── Todos ────────────────────────────────────────────────────

	async getTodos() {
		return this.get<FreeApiResponse<Todo[]>>('todos', {
			failOnStatusCode: false,
		});
	}

	async createTodo(title: string, description: string) {
		return this.post<FreeApiResponse<Todo>>('todos', {
			data: { title, description },
			failOnStatusCode: false,
		});
	}

	async getTodoById(id: string) {
		return this.get<FreeApiResponse<Todo>>(`todos/${id}`, {
			failOnStatusCode: false,
		});
	}

	async updateTodo(id: string, data: { title?: string; description?: string }) {
		return this.patch<FreeApiResponse<Todo>>(`todos/${id}`, {
			data,
			failOnStatusCode: false,
		});
	}

	async toggleTodoStatus(id: string) {
		return this.patch<FreeApiResponse<Todo>>(`todos/toggle/status/${id}`, {
			failOnStatusCode: false,
		});
	}

	async deleteTodo(id: string) {
		return this.delete<FreeApiResponse<{ deletedTodo: Todo }>>(`todos/${id}`, {
			failOnStatusCode: false,
		});
	}
}
