import { faker } from '@faker-js/faker';
import type { Book } from '@/types/freeapi';

/**
 * Builds a `Book` object matching the Zod-typed FreeAPI book shape (Google
 * Books style envelope). Pass `overrides` to pin any field; nested `volumeInfo`
 * overrides are shallow-merged onto the generated defaults.
 */
export function buildBook(overrides: Partial<Book> = {}): Book {
	const { volumeInfo: volumeInfoOverride, ...rootOverrides } = overrides;
	const id = faker.number.int({ min: 1, max: 100_000 });
	const thumbnail = faker.image.urlLoremFlickr({ category: 'book' });

	return {
		kind: 'books#volume',
		id,
		etag: faker.string.alphanumeric({ length: 12 }),
		selfLink: `https://www.googleapis.com/books/v1/volumes/${id}`,
		volumeInfo: {
			title: faker.book.title(),
			authors: [faker.book.author()],
			publisher: faker.book.publisher(),
			publishedDate: faker.date.past().toISOString().slice(0, 10),
			description: faker.lorem.paragraph(),
			pageCount: faker.number.int({ min: 50, max: 1200 }),
			printType: 'BOOK',
			categories: [faker.book.genre()],
			averageRating: faker.number.float({ min: 0, max: 5, fractionDigits: 1 }),
			ratingsCount: faker.number.int({ min: 0, max: 5000 }),
			maturityRating: 'NOT_MATURE',
			imageLinks: {
				smallThumbnail: thumbnail,
				thumbnail,
			},
			language: 'en',
			...volumeInfoOverride,
		},
		...rootOverrides,
	};
}
