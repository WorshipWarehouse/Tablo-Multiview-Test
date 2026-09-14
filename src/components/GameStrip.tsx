import React from 'react';
import { Volume2, ArrowLeftRight } from 'lucide-react';
import { PaneState, MultiviewLayoutMode } from '../types';

interface GameStripProps {
  layoutMode: MultiviewLayoutMode;
  activePaneIndex: number;
  panes: PaneState[];
  onSelectPane: (index: number) => void;
  onChangeGame: (index: number) => void;
}

export const GameStrip: React.FC<GameStripProps> = ({
  layoutMode,
  activePaneIndex,
  panes,
  onSelectPane,
  onChangeGame,
}) => {
  const count =
    layoutMode === 'ONE_PANE'
      ? 1
      : layoutMode === 'TWO_PANE'
      ? 2
      : layoutMode === 'THREE_PANE'
      ? 3
      : 4;

  const visiblePanes = panes.slice(0, count);

  return (
    <div className="h-14 w-full bg-[#0c0c0c] border-t border-zinc-900 px-4 flex items-center justify-between gap-2 select-none z-30">
      {/* Game Chips Grid */}
      <div className="flex-1 flex items-center gap-2 overflow-x-auto no-scrollbar">
        {visiblePanes.map((pane, idx) => {
          const isActive = idx === activePaneIndex;
          const channel = pane.channel;
          const title = channel?.liveEventTitle || 'Live Event';
          const network = channel ? `${channel.network} ${channel.major}.${channel.minor}` : `Game ${idx + 1}`;
          const score = channel?.scoreBug;

          return (
            <div
              key={idx}
              onClick={() => onSelectPane(idx)}
              className={`flex-1 min-w-[200px] max-w-[340px] h-10 px-3 flex items-center justify-between rounded-lg cursor-pointer transition border ${
                isActive
                  ? 'bg-zinc-800/90 border-zinc-400 text-white shadow-md'
                  : 'bg-zinc-900/60 border-zinc-800/60 text-zinc-400 hover:bg-zinc-800/60 hover:text-zinc-200'
              }`}
            >
              <div className="flex items-center gap-2 min-w-0 pr-2">
                {/* Audio or Game Number indicator */}
                <div
                  className={`w-5 h-5 rounded flex items-center justify-center text-[11px] font-bold shrink-0 ${
                    isActive
                      ? 'bg-white text-black'
                      : 'bg-zinc-800 text-zinc-400'
                  }`}
                >
                  {isActive ? <Volume2 className="w-3.5 h-3.5" /> : idx + 1}
                </div>

                {/* Game Matchup / Channel Info */}
                <div className="flex flex-col min-w-0">
                  <div className="flex items-center gap-1.5 leading-none">
                    <span className="text-xs font-semibold truncate text-zinc-200">
                      {network}
                    </span>
                    {score && (
                      <span className="text-[10px] text-zinc-400 truncate hidden sm:inline">
                        • {score}
                      </span>
                    )}
                  </div>
                  <span className="text-[11px] text-zinc-400 truncate mt-0.5">
                    {title}
                  </span>
                </div>
              </div>

              {/* Quick Swap Game Button */}
              <button
                onClick={(e) => {
                  e.stopPropagation();
                  onChangeGame(idx);
                }}
                className="p-1 hover:bg-zinc-700 rounded text-zinc-400 hover:text-white transition shrink-0"
                title={`Change Game ${idx + 1}`}
              >
                <ArrowLeftRight className="w-3.5 h-3.5" />
              </button>
            </div>
          );
        })}
      </div>

      {/* Helpful Hint */}
      <div className="hidden xl:flex items-center gap-2 text-zinc-500 text-xs shrink-0 pl-2">
        <span>Click any game to hear audio</span>
        <span>•</span>
        <span>Double-click to full-screen</span>
      </div>
    </div>
  );
};
