type Values<T extends {}> = T[keyof T];

type Tuplize<T extends {}[]> = Pick<T, Exclude<keyof T, Extract<keyof {}[], string> | number>>;

type _OneOf<T extends {}> = Values<{
    [K in keyof T]: T[K] & {
    [M in Values<{ [L in keyof Omit<T, K>]: keyof T[L] }>]?: undefined
}
}>;

export type OneOf<T extends {}[]> = _OneOf<Tuplize<T>>;
