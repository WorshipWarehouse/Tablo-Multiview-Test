import React, { useEffect, useRef, useState } from 'react';
import Hls from 'hls.js';
import { PaneState } from '../types';

interface MultiviewPlayerPaneProps {
  paneState: PaneState;
  isActive: boolean;
  onFocus: () => void;
  onDoubleClick: () => void;
}

export const MultiviewPlayerPane: React.FC<MultiviewPlayerPaneProps> = ({
  paneState,
  isActive,
  onFocus,
  onDoubleClick,
}) => {
  const videoRef = useRef<HTMLVideoElement | null>(null);
  const hlsRef = useRef<Hls | null>(null);
  const [isLoading, setIsLoading] = useState(true);

  const streamUrl = paneState.streamUrl || paneState.channel?.streamUrl;

  useEffect(() => {
    const video = videoRef.current;
    if (!video || !streamUrl) return;

    setIsLoading(true);

    if (hlsRef.current) {
      hlsRef.current.destroy();
      hlsRef.current = null;
    }

    const isHls = streamUrl.includes('.m3u8');

    if (isHls && Hls.isSupported()) {
      const hls = new Hls({
        enableWorker: true,
        lowLatencyMode: true,
        autoStartLoad: true,
      });
      hlsRef.current = hls;

      hls.loadSource(streamUrl);
      hls.attachMedia(video);

      hls.on(Hls.Events.MANIFEST_PARSED, () => {
        video.play().catch(() => {
          video.muted = true;
          video.play().catch(() => {});
        });
      });

      hls.on(Hls.Events.ERROR, (_, data) => {
        if (data.fatal) {
          // Fallback to direct video load if HLS fails
          video.src = streamUrl;
          video.play().catch(() => {});
        }
      });
    } else {
      // Direct MP4 or native playback
      video.src = streamUrl;
      video.load();
      video.play().catch(() => {
        video.muted = true;
        video.play().catch(() => {});
      });
    }

    return () => {
      if (hlsRef.current) {
        hlsRef.current.destroy();
        hlsRef.current = null;
      }
    };
  }, [streamUrl]);

  // Audio management: Unmute only when this pane is active and not muted
  useEffect(() => {
    const video = videoRef.current;
    if (!video) return;

    if (isActive && !paneState.isMuted) {
      video.muted = false;
      video.volume = 1.0;
    } else {
      video.muted = true;
    }
  }, [isActive, paneState.isMuted]);

  return (
    <div
      onClick={onFocus}
      onDoubleClick={onDoubleClick}
      className={`relative w-full h-full bg-black overflow-hidden cursor-pointer transition-all duration-150 select-none ${
        isActive
          ? 'outline outline-[1.5px] outline-white -outline-offset-[1.5px] z-20 shadow-xl'
          : 'outline outline-[0.5px] outline-black z-0 opacity-95 hover:opacity-100'
      }`}
    >
      {/* 100% Clean Video Stream - Zero Overlays - Full Edge-to-Edge */}
      <video
        ref={videoRef}
        playsInline
        autoPlay
        loop
        muted={!isActive || paneState.isMuted}
        onPlaying={() => setIsLoading(false)}
        onWaiting={() => setIsLoading(true)}
        className="w-full h-full object-cover bg-black pointer-events-none"
      />

      {/* Brief Buffering Indicator (vanishes automatically once playing) */}
      {isLoading && (
        <div className="absolute inset-0 flex items-center justify-center bg-black/40 pointer-events-none">
          <div className="w-8 h-8 border-2 border-white/80 border-t-transparent rounded-full animate-spin" />
        </div>
      )}
    </div>
  );
};
