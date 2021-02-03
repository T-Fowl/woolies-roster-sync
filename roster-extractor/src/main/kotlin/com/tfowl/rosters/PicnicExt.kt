package com.tfowl.rosters

import com.jakewharton.picnic.Table

internal val Table.PositionedCell.content: String get() = cell.content

internal fun Table.firstCell(predicate: (Table.PositionedCell) -> Boolean): Table.PositionedCell =
    positionedCells.first(predicate)

internal fun Table.firstCellOrNull(predicate: (Table.PositionedCell) -> Boolean): Table.PositionedCell? =
    positionedCells.firstOrNull(predicate)

internal fun Table.get(
    origin: Table.PositionedCell,
    rowOffset: Int = 0,
    colOffset: Int = 0,
): Table.PositionedCell =
    get(origin.rowIndex + rowOffset, origin.columnIndex + colOffset)

internal operator fun Table.get(rows: Iterable<Int>, col: Int): List<Table.PositionedCell> = rows.map { get(it, col) }

internal operator fun Table.get(row: Int, cols: Iterable<Int>): List<Table.PositionedCell> = cols.map { get(row, it) }
