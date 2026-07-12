import { faker } from '@faker-js/faker';

/** Payload accepted by `FreeApiClient.createTodo`. */
export interface TodoInput {
	title: string;
	description: string;
}

/**
 * Builds a todo creation payload with realistic, unique content. Pass
 * `overrides` to pin the title or description.
 */
export function buildTodo(overrides: Partial<TodoInput> = {}): TodoInput {
	return {
		title: faker.lorem.sentence({ min: 3, max: 6 }),
		description: faker.lorem.sentences({ min: 1, max: 3 }),
		...overrides,
	};
}
