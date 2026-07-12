import { faker } from '@faker-js/faker';

/**
 * Test-data factory layer.
 *
 * Typed builders that produce the domain objects used across the API/FreeAPI
 * suites. Every builder accepts partial `overrides` and, by default, emits
 * unique values (safe for live create/register calls). Call `seedFactories`
 * with a fixed number to make output deterministic for reproducible runs.
 */

export { buildBook } from './book.factory';
export { buildProduct } from './product.factory';
export {
	buildRegistration,
	type RegistrationInput,
} from './registration.factory';
export { buildTodo, type TodoInput } from './todo.factory';

/**
 * Seed the shared faker instance for deterministic factory output.
 * Call with no argument to restore non-deterministic (random) generation.
 */
export function seedFactories(seed?: number): void {
	faker.seed(seed);
}
