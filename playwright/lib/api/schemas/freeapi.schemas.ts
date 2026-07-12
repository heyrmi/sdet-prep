import { z } from 'zod';

// ── Generic Envelope Factories ───────────────────────────────────

export function freeApiEnvelopeSchema<T extends z.ZodTypeAny>(dataSchema: T) {
	return z.object({
		statusCode: z.number(),
		data: dataSchema,
		message: z.string(),
		success: z.literal(true),
	});
}

export function paginatedDataSchema<T extends z.ZodTypeAny>(itemSchema: T) {
	return z.object({
		page: z.number().int().positive(),
		limit: z.number().int().positive(),
		totalPages: z.number().int().nonnegative(),
		previousPage: z.boolean(),
		nextPage: z.boolean(),
		totalItems: z.number().int().nonnegative(),
		currentPageItems: z.number().int().nonnegative(),
		data: z.array(itemSchema),
	});
}

export const freeApiErrorSchema = z.object({
	statusCode: z.number(),
	data: z.null(),
	message: z.string().min(1),
	success: z.literal(false),
	errors: z.array(z.record(z.string(), z.string())),
});

// ── Random Users ─────────────────────────────────────────────────

export const randomUserSchema = z.object({
	id: z.number(),
	gender: z.string(),
	name: z.object({
		title: z.string(),
		first: z.string(),
		last: z.string(),
	}),
	location: z.object({
		street: z.object({ number: z.number(), name: z.string() }),
		city: z.string(),
		state: z.string(),
		country: z.string(),
		postcode: z.union([z.number(), z.string()]),
		coordinates: z.object({ latitude: z.string(), longitude: z.string() }),
		timezone: z.object({ offset: z.string(), description: z.string() }),
	}),
	email: z.email(),
	login: z.object({
		uuid: z.uuid(),
		username: z.string(),
		password: z.string(),
		salt: z.string(),
		md5: z.string(),
		sha1: z.string(),
		sha256: z.string(),
	}),
	dob: z.object({ date: z.string(), age: z.number() }),
	registered: z.object({ date: z.string(), age: z.number() }),
	phone: z.string(),
	cell: z.string(),
	picture: z.object({
		large: z.url(),
		medium: z.url(),
		thumbnail: z.url(),
	}),
	nat: z.string(),
});

// ── Random Products ──────────────────────────────────────────────

export const productSchema = z.object({
	id: z.number(),
	title: z.string(),
	description: z.string(),
	price: z.number().positive(),
	discountPercentage: z.number().nonnegative(),
	rating: z.number().min(0).max(5),
	stock: z.number().nonnegative(),
	brand: z.string(),
	category: z.string(),
	thumbnail: z.url(),
	images: z.array(z.url()).min(1),
});

// ── Random Jokes ─────────────────────────────────────────────────

export const jokeSchema = z.object({
	id: z.number(),
	categories: z.array(z.string()),
	content: z.string().min(1),
});

// ── Books ────────────────────────────────────────────────────────

export const bookSchema = z.object({
	kind: z.string(),
	id: z.number(),
	etag: z.string(),
	selfLink: z.url(),
	volumeInfo: z.looseObject({
		title: z.string(),
		subtitle: z.string().optional(),
		authors: z.array(z.string()).min(1),
		publisher: z.string().optional(),
		publishedDate: z.string(),
		description: z.string().optional(),
		pageCount: z.number().nonnegative().optional(),
		printType: z.string(),
		categories: z.array(z.string()),
		averageRating: z.number().optional(),
		ratingsCount: z.number().optional(),
		maturityRating: z.string(),
		imageLinks: z
			.object({ smallThumbnail: z.string(), thumbnail: z.string() })
			.optional(),
		language: z.string(),
	}),
});

// ── Stocks ───────────────────────────────────────────────────────

export const stockSchema = z.object({
	Name: z.string(),
	Symbol: z.string(),
	ListingDate: z.string(),
	ISIN: z.string(),
	MarketCap: z.string(),
	CurrentPrice: z.string(),
	HighLow: z.string(),
	StockPE: z.string(),
	BookValue: z.string(),
	DividendYield: z.string(),
	ROCE: z.string(),
	ROE: z.string(),
	FaceValue: z.string(),
});

// ── Auth ─────────────────────────────────────────────────────────

export const authUserSchema = z.object({
	_id: z.string(),
	avatar: z.object({
		url: z.string(),
		localPath: z.string(),
		_id: z.string(),
	}),
	username: z.string(),
	email: z.email(),
	role: z.string(),
	loginType: z.string(),
	isEmailVerified: z.boolean(),
	createdAt: z.string(),
	updatedAt: z.string(),
});

export const registerResponseSchema = z.object({
	user: authUserSchema,
});

export const loginResponseSchema = z.object({
	user: authUserSchema,
	accessToken: z.string().min(1),
	refreshToken: z.string().min(1),
});

// ── Todos ────────────────────────────────────────────────────────

export const todoSchema = z.object({
	_id: z.string(),
	title: z.string(),
	description: z.string(),
	isComplete: z.boolean(),
	createdAt: z.string(),
	updatedAt: z.string(),
});
