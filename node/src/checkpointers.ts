/*
 * Copyright 2022 IBM All Rights Reserved.
 *
 * SPDX-License-Identifier: Apache-2.0
 */

import { Checkpointer } from '.';
import { FileCheckpointer } from './filecheckpointer';

/**
 * Create a checkpointer that uses the specified file to store persistent state.
 * @param path - Path to a file holding persistent checkpoint state.
 */
export function file(path: string): Promise<Checkpointer> {
    return FileCheckpointer.newInstance(path);
}
