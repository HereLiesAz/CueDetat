// src/components/CameraFeed.js
import React, { useEffect, useRef } from 'react';

const CameraFeed = ({ onStreamReady, onError }) => {
    const videoRef = useRef(null);

    useEffect(() => {
        let stream;
        const enableStream = async () => {
            try {
                stream = await navigator.mediaDevices.getUserMedia({
                    video: { facingMode: 'environment' }
                });
                if (videoRef.current) {
                    videoRef.current.srcObject = stream;
                    onStreamReady();
                }
            } catch (err) {
                console.error("Error accessing camera: ", err);
                onError(err);
            }
        };

        enableStream();

        return () => {
            if (stream) {
                stream.getTracks().forEach(track => track.stop());
            }
        };
    }, [onStreamReady, onError]);

    return (
        <video
            ref={videoRef}
            id="camera-feed"
            autoPlay
            playsInline
            muted
        ></video>
    );
};

export default CameraFeed;