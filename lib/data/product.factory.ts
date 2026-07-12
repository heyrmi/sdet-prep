import { faker } from '@faker-js/faker';
import type { Product } from '@/types/freeapi';

/**
 * Builds a `Product` object matching the Zod-typed FreeAPI product shape.
 * Useful for stubbing request bodies / contract expectations without hitting
 * the live catalogue. Pass `overrides` to pin any field.
 */
export function buildProduct(overrides: Partial<Product> = {}): Product {
	const thumbnail = faker.image.urlLoremFlickr({ category: 'product' });

	return {
		id: faker.number.int({ min: 1, max: 100_000 }),
		title: faker.commerce.productName(),
		description: faker.commerce.productDescription(),
		price: Number(faker.commerce.price({ min: 1, max: 2000 })),
		discountPercentage: faker.number.float({
			min: 0,
			max: 40,
			fractionDigits: 2,
		}),
		rating: faker.number.float({ min: 0, max: 5, fractionDigits: 2 }),
		stock: faker.number.int({ min: 0, max: 500 }),
		brand: faker.company.name(),
		category: faker.commerce.department(),
		thumbnail,
		images: [thumbnail, faker.image.urlLoremFlickr({ category: 'product' })],
		...overrides,
	};
}
