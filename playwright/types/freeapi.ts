// ── Common Envelope ──────────────────────────────────────────────

export interface FreeApiResponse<T> {
	statusCode: number;
	data: T;
	message: string;
	success: true;
}

export interface FreeApiError {
	statusCode: number;
	data: null;
	message: string;
	success: false;
	errors: Record<string, string>[];
}

export interface PaginatedData<T> {
	page: number;
	limit: number;
	totalPages: number;
	previousPage: boolean;
	nextPage: boolean;
	totalItems: number;
	currentPageItems: number;
	data: T[];
}

// ── Random Users ─────────────────────────────────────────────────

export interface RandomUser {
	id: number;
	gender: string;
	name: { title: string; first: string; last: string };
	location: {
		street: { number: number; name: string };
		city: string;
		state: string;
		country: string;
		postcode: number | string;
		coordinates: { latitude: string; longitude: string };
		timezone: { offset: string; description: string };
	};
	email: string;
	login: {
		uuid: string;
		username: string;
		password: string;
		salt: string;
		md5: string;
		sha1: string;
		sha256: string;
	};
	dob: { date: string; age: number };
	registered: { date: string; age: number };
	phone: string;
	cell: string;
	picture: { large: string; medium: string; thumbnail: string };
	nat: string;
}

// ── Random Products ──────────────────────────────────────────────

export interface Product {
	id: number;
	title: string;
	description: string;
	price: number;
	discountPercentage: number;
	rating: number;
	stock: number;
	brand: string;
	category: string;
	thumbnail: string;
	images: string[];
}

// ── Random Jokes ─────────────────────────────────────────────────

export interface Joke {
	id: number;
	categories: string[];
	content: string;
}

// ── Books ────────────────────────────────────────────────────────

export interface Book {
	kind: string;
	id: number;
	etag: string;
	selfLink: string;
	volumeInfo: {
		title: string;
		subtitle?: string;
		authors: string[];
		publisher?: string;
		publishedDate: string;
		description?: string;
		pageCount?: number;
		printType: string;
		categories: string[];
		averageRating?: number;
		ratingsCount?: number;
		maturityRating: string;
		imageLinks?: { smallThumbnail: string; thumbnail: string };
		language: string;
	};
}

// ── Stocks ───────────────────────────────────────────────────────

export interface Stock {
	Name: string;
	Symbol: string;
	ListingDate: string;
	ISIN: string;
	MarketCap: string;
	CurrentPrice: string;
	HighLow: string;
	StockPE: string;
	BookValue: string;
	DividendYield: string;
	ROCE: string;
	ROE: string;
	FaceValue: string;
}

// ── Auth ─────────────────────────────────────────────────────────

export interface AuthUser {
	_id: string;
	avatar: { url: string; localPath: string; _id: string };
	username: string;
	email: string;
	role: string;
	loginType: string;
	isEmailVerified: boolean;
	createdAt: string;
	updatedAt: string;
}

export interface RegisterResponse {
	user: AuthUser;
}

export interface LoginResponse {
	user: AuthUser;
	accessToken: string;
	refreshToken: string;
}

// ── Todos ────────────────────────────────────────────────────────

export interface Todo {
	_id: string;
	title: string;
	description: string;
	isComplete: boolean;
	createdAt: string;
	updatedAt: string;
}
