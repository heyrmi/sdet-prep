import { faker } from '@faker-js/faker';

/**
 * Credentials payload accepted by `FreeApiClient.register` / `login`.
 * FreeAPI requires a lowercase, space-free username and a password that mixes
 * upper/lower case with a digit, so the factory guarantees those constraints.
 */
export interface RegistrationInput {
	email: string;
	username: string;
	password: string;
}

/**
 * Builds a unique registration payload. By default the username/email carry
 * random entropy so repeated live registrations never collide; seed the shared
 * faker instance via `seedFactories` for deterministic output, and pass
 * `overrides` to pin any field.
 */
export function buildRegistration(
	overrides: Partial<RegistrationInput> = {},
): RegistrationInput {
	const suffix = faker.string.alphanumeric({ length: 10, casing: 'lower' });
	const username = `pw_${suffix}`;

	return {
		email: `${username}@example.com`,
		username,
		password: `Pw${faker.string.alphanumeric({ length: 8 })}123!`,
		...overrides,
	};
}
