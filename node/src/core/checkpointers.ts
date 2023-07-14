/*
 * Copyright 2022 IBM All Rights Reserved.
 *
 * SPDX-License-Identifier: Apache-2.0
 */

import { Checkpointer } from './checkpointer';
import { InMemoryCheckPointer } from './inmemorycheckpointer';

/**
 * Create a checkpointer that stores its state in memory only.
 */
export function inMemory(): Checkpointer {
    return new InMemoryCheckPointer();
}
